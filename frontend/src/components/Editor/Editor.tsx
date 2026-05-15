import { useEffect, useRef, useCallback, useState } from "react";
import * as Y from "yjs";
import { Awareness } from "y-protocols/awareness";
import TextareaCodeEditor from "@uiw/react-textarea-code-editor";
import "@uiw/react-textarea-code-editor/dist.css";

import { Crepe } from "@milkdown/crepe";

import { Editor, editorStateCtx, editorViewCtx, prosePluginsCtx, EditorStateReady } from "@milkdown/kit/core";
import { listenerCtx } from "@milkdown/kit/plugin/listener";
import { collab, collabServiceCtx, CollabReady, CollabService } from "@milkdown/plugin-collab";
import { Milkdown, MilkdownProvider, useEditor } from "@milkdown/react";
import { getMarkdown } from "@milkdown/utils";

import { useCollabProvider } from "../../hooks/useCollabProvider";
import type { CollabUser, CollabStatus } from "../../hooks/useCollabProvider";

// Re-export for consumers
export type { CollabUser, CollabStatus };

function decodeHtmlEntities(text: string): string {
  return text
    .replace(/&#x([0-9a-fA-F]+);/g, (_, hex: string) => String.fromCodePoint(Number.parseInt(hex, 16)))
    .replace(/&#(\d+);/g, (_, dec: string) => String.fromCodePoint(Number.parseInt(dec, 10)))
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&(apos|#39);/g, "'")
    .replace(/&amp;/g, "&");
}

function normalizeHighlightedCodeFences(md: string): string {
  return md.replace(/```([^\n`]*)\n([\s\S]*?)```/g, (_match, lang: string, code: string) => {
    // Only strip highlight.js <span class="hljs-...">...</span> wrappers, not generic <...> like C++ includes
    if (!/<span\s+class\s*=\s*["']hljs-/i.test(code)) {
      return `\`\`\`${lang}\n${code}\`\`\``;
    }

    const normalizedCode = decodeHtmlEntities(
      code
        .replace(/<span\s+class\s*=\s*["']hljs-[^"']*["']\s*>/gi, "")
        .replace(/<\/span>/gi, ""),
    );
    return `\`\`\`${lang}\n${normalizedCode}\`\`\``;
  });
}

interface EditorProps {
  documentId: string;
  username: string;
  initialContent?: string;
  onChange?: (markdown: string) => void;
  onCollabStatusChange?: (status: CollabStatus) => void;
  onCollabUsersChange?: (users: CollabUser[]) => void;
  sourceMode?: boolean;
}

function cursorBuilder(user: { name: string; color: string }): HTMLElement {
  const el = document.createElement("span");
  el.className = "collab-cursor";
  el.style.borderLeft = `2px solid ${user.color}`;
  el.style.marginLeft = "-1px";
  el.style.position = "relative";
  el.style.userSelect = "none";

  const label = document.createElement("span");
  label.className = "collab-cursor-label";
  label.textContent = user.name;
  label.style.position = "absolute";
  label.style.top = "-18px";
  label.style.left = "-1px";
  label.style.fontSize = "10px";
  label.style.lineHeight = "1";
  label.style.padding = "1px 4px";
  label.style.borderRadius = "3px 3px 3px 0";
  label.style.backgroundColor = user.color;
  label.style.color = "#fff";
  label.style.whiteSpace = "nowrap";
  label.style.pointerEvents = "none";
  label.style.userSelect = "none";

  el.appendChild(label);
  return el;
}

export default function MarkdownEditor({
  documentId,
  username,
  initialContent,
  onChange,
  onCollabStatusChange,
  onCollabUsersChange,
  sourceMode = false,
}: EditorProps) {
  const [sourceContent, setSourceContent] = useState(() => initialContent || "");
  const sourceContentRef = useRef(initialContent || "");
  const [shouldBootstrap, setShouldBootstrap] = useState<boolean | null>(null);

  useEffect(() => {
    let cancelled = false;
    setShouldBootstrap(null);
    fetch(`/api/collaboration-meta/claim-bootstrap?docId=${encodeURIComponent(documentId)}`, {
      credentials: "include",
    })
      .then((res) => res.ok ? res.json() : Promise.reject(new Error(`HTTP ${res.status}`)))
      .then((res) => {
        if (cancelled) return;
        setShouldBootstrap(Boolean(res?.data));
      })
      .catch(() => {
        if (!cancelled) setShouldBootstrap(true);
      });
    return () => { cancelled = true; };
  }, [documentId]);

  // Collab provider hook — owns Yjs Doc, Awareness, WebSocket lifecycle
  const { yDoc, awareness, provider, status, users } = useCollabProvider(documentId, username, shouldBootstrap !== null);

  // Milkdown refs
  const editorRef = useRef<Editor | null>(null);
  const collabServiceRef = useRef<CollabService | null>(null);

  const documentIdRef = useRef(documentId);

  useEffect(() => {
    if (documentId !== documentIdRef.current) {
      documentIdRef.current = documentId;
    }
  }, [documentId]);

  // Stable callback refs
  const onChangeRef = useRef(onChange);
  const onCollabStatusChangeRef = useRef(onCollabStatusChange);
  const onCollabUsersChangeRef = useRef(onCollabUsersChange);

  useEffect(() => {
    onChangeRef.current = onChange;
  }, [onChange]);
  useEffect(() => {
    onCollabStatusChangeRef.current = onCollabStatusChange;
  }, [onCollabStatusChange]);
  useEffect(() => {
    onCollabUsersChangeRef.current = onCollabUsersChange;
  }, [onCollabUsersChange]);

  // Initial content ref (stable, doesn't trigger re-renders)
  const initialContentRef = useRef(initialContent);

  // ---- Forward collab status/users to parent ----
  useEffect(() => {
    onCollabStatusChangeRef.current?.(status);
  }, [status]);

  useEffect(() => {
    onCollabUsersChangeRef.current?.(users);
  }, [users]);

  // ---- Sync source ↔ milkdown when switching modes ----
  const prevSourceModeRef = useRef(sourceMode);

  useEffect(() => {
    // WYSIWYG → source: grab current markdown from milkdown
    if (sourceMode && !prevSourceModeRef.current) {
      const editor = editorRef.current;
      if (editor && collabServiceRef.current) {
        try {
          const md = normalizeHighlightedCodeFences(editor.action(getMarkdown()) ?? "");
          sourceContentRef.current = md;
          setSourceContent(md);
        } catch { /* editor not ready */ }
      }
    }
    // source → WYSIWYG: apply source content to milkdown via Yjs
    if (!sourceMode && prevSourceModeRef.current) {
      const cs = collabServiceRef.current;
      const content = sourceContentRef.current;
      if (cs && content != null) {
        cs.applyTemplate(content, () => true);
      }
    }
    prevSourceModeRef.current = sourceMode;
  }, [sourceMode]);

  // ---- Source mode textarea handler ----
  const handleSourceChange = useCallback((e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const val = e.target.value;
    sourceContentRef.current = val;
    setSourceContent(val);
    onChangeRef.current?.(val);
  }, []);

  return (
    <div className="flex-1 min-h-0 flex flex-col relative">
      {/* WYSIWYG milkdown editor */}
      <div className={`flex-1 min-h-0 overflow-hidden ${sourceMode ? "absolute inset-0 opacity-0 pointer-events-none" : ""}`}>
        {yDoc && awareness && shouldBootstrap !== null && (
          <MilkdownProvider>
            <MilkdownEditorInner
              yDoc={yDoc}
              awareness={awareness}
              editorRef={editorRef}
              collabServiceRef={collabServiceRef}
              onChangeRef={onChangeRef}
              initialContent={initialContentRef.current || ""}
              shouldBootstrap={shouldBootstrap}
            />
          </MilkdownProvider>
        )}
      </div>

      {/* Source code editor */}
      <div className={`flex-1 min-h-0 overflow-auto pb-40 ${!sourceMode ? "absolute inset-0 opacity-0 pointer-events-none" : ""}`} data-color-mode={document.documentElement.classList.contains("dark") ? "dark" : "light"}>
        <TextareaCodeEditor
          value={sourceContent}
          language="markdown"
          placeholder="在此输入 Markdown 源码..."
          padding={24}
          minHeight={600}
          style={{
            fontSize: 20,
            fontFamily: 'var(--font-mono)',
            backgroundColor: "var(--color-surface-container-lowest)",
            color: "var(--color-on-surface)",
            lineHeight: "32px",
            minHeight: "100%",
          }}
          onChange={handleSourceChange}
        />
      </div>
    </div>
  );
}

// ---- Milkdown inner component ----

interface MilkdownEditorInnerProps {
  yDoc: Y.Doc;
  awareness: Awareness;
  editorRef: React.MutableRefObject<Editor | null>;
  collabServiceRef: React.MutableRefObject<CollabService | null>;
  onChangeRef: React.MutableRefObject<((md: string) => void) | undefined>;
  initialContent: string;
  shouldBootstrap: boolean;
}

function MilkdownEditorInner({
  yDoc,
  awareness,
  editorRef,
  collabServiceRef,
  onChangeRef,
  initialContent,
  shouldBootstrap,
}: MilkdownEditorInnerProps) {
  const aliveRef = useRef(true);
  const editorReadyRef = useRef(false);

  // Stable refs so useEditor factory does NOT recreate when yDoc/awareness mutate
  const yDocRef = useRef(yDoc);
  yDocRef.current = yDoc;
  const awarenessRef = useRef(awareness);
  awarenessRef.current = awareness;

  // Observe Yjs doc changes and forward to onChange.
  useEffect(() => {
    const fragment = yDoc.getXmlFragment("prosemirror");
    let timer: ReturnType<typeof setTimeout> | null = null;

    const observer = () => {
      if (!aliveRef.current) return;
      if (timer) clearTimeout(timer);
      timer = setTimeout(() => {
        const ed = editorRef.current;
        if (!ed || !editorReadyRef.current) return;
        try {
          const md = normalizeHighlightedCodeFences(ed.action(getMarkdown()) ?? "");
          onChangeRef.current?.(md);
        } catch { /* editor not ready */ }
      }, 100);
    };

    fragment.observeDeep(observer);
    return () => {
      fragment.unobserveDeep(observer);
      if (timer) clearTimeout(timer);
    };
  }, [yDoc, editorRef, onChangeRef]);

  const { get } = useEditor((root) => {
    aliveRef.current = true;

    const crepe = new Crepe({
      root,
      defaultValue: "",
      features: {
        [Crepe.Feature.BlockEdit]: true,
        [Crepe.Feature.Toolbar]: true,
        [Crepe.Feature.LinkTooltip]: true,
        [Crepe.Feature.ImageBlock]: true,
        [Crepe.Feature.CodeMirror]: true,
        [Crepe.Feature.ListItem]: true,
        [Crepe.Feature.Table]: true,
        [Crepe.Feature.Cursor]: true,
        [Crepe.Feature.Placeholder]: true,
      },
    });

    crepe.editor
      .use(collab)
      .config((ctx) => {
        ctx.get(listenerCtx).markdownUpdated((_ctx, markdown) => {
          onChangeRef.current?.(normalizeHighlightedCodeFences(markdown));
        });

        ctx.wait(CollabReady).then(async () => {
          if (!aliveRef.current) return;
          try {
            await ctx.wait(EditorStateReady);
            if (!aliveRef.current || !ctx.isInjected(editorStateCtx)) return;

            editorReadyRef.current = true;
            const collabService = ctx.get(collabServiceCtx);
            collabServiceRef.current = collabService;

            collabService
              .bindDoc(yDocRef.current)
              .setAwareness(awarenessRef.current)
              .setOptions({
                yCursorOpts: {
                  cursorBuilder: (user: { name: string; color: string }) => cursorBuilder(user),
                },
              });

            if (shouldBootstrap && initialContent) {
              collabService.applyTemplate(initialContent);
            }

            collabService.connect();

            // Remove ProseMirror's history plugin — yUndoPlugin handles undo/redo in collab mode.
            // Without this, PM history records remote sync transactions as empty stack items.
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const plugins = ctx.get(prosePluginsCtx).filter(
              (p) => !((p as any).key?.key?.startsWith?.("history"))
            );
            ctx.set(prosePluginsCtx, plugins);
            const view = ctx.get(editorViewCtx);
            view.updateState(view.state.reconfigure({ plugins }));
          } catch (error) {
            if (aliveRef.current) {
              console.warn("Collaboration setup skipped", error);
            }
          }
        });
      });

    return crepe;
  }, []);

  useEffect(() => {
    const ed = get();
    if (ed) editorRef.current = ed;
  }, [get, editorRef]);

  useEffect(() => {
    return () => {
      aliveRef.current = false;
      editorReadyRef.current = false;
      editorRef.current = null;
      collabServiceRef.current = null;
    };
  }, []);

  return <Milkdown />;
}
