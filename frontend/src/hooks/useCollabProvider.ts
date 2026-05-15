import { useEffect, useRef, useState } from "react";
import * as Y from "yjs";
import { WebsocketProvider } from "y-websocket";
import { Awareness } from "y-protocols/awareness";

export type CollabStatus = "connecting" | "connected" | "disconnected";

export interface CollabUser {
  name: string;
  color: string;
  clientId: number;
}

export interface UseCollabProviderReturn {
  yDoc: Y.Doc | null;
  awareness: Awareness | null;
  provider: WebsocketProvider | null;
  status: CollabStatus;
  users: CollabUser[];
}

const CURSOR_COLORS = [
  "#f87171", "#fb923c", "#fbbf24", "#a3e635",
  "#34d399", "#22d3ee", "#60a5fa", "#a78bfa",
  "#f472b6", "#e879f9",
];

function pickColor(name: string): string {
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
  return CURSOR_COLORS[Math.abs(hash) % CURSOR_COLORS.length]!;
}

function buildCollabUsers(awareness: Awareness, username: string): CollabUser[] {
  const users: CollabUser[] = [];
  awareness.getStates().forEach((state: Record<string, { name: string; color: string }>, clientId: number) => {
    if (!state?.user) return;
    users.push({ name: state.user.name, color: state.user.color, clientId });
  });
  users.sort((a, b) => {
    const aIsLocal = a.name === username;
    const bIsLocal = b.name === username;
    if (aIsLocal && !bIsLocal) return -1;
    if (!aIsLocal && bIsLocal) return 1;
    return a.clientId - b.clientId;
  });
  return users;
}

/**
 * 管理 Yjs Doc + Awareness + WebSocket Provider 的生命周期。
 *
 * - documentId 变化时销毁旧资源、创建新资源
 * - username 变化时更新 awareness 本地状态
 * - 自动重连（3s 间隔）
 * - 组件卸载时完整清理
 */
export function useCollabProvider(
  documentId: string,
  username: string,
  enabled = true,
): UseCollabProviderReturn {
  const [status, setStatus] = useState<CollabStatus>("disconnected");
  const [users, setUsers] = useState<CollabUser[]>([]);
  const [yDoc, setYDoc] = useState<Y.Doc | null>(null);
  const [awareness, setAwareness] = useState<Awareness | null>(null);
  const [provider, setProvider] = useState<WebsocketProvider | null>(null);

  // refs for stable access inside closures
  const yDocRef = useRef<Y.Doc | null>(null);
  const awarenessRef = useRef<Awareness | null>(null);
  const providerRef = useRef<WebsocketProvider | null>(null);
  const retryTimerRef = useRef<number | null>(null);
  const connectionIdRef = useRef(0);
  const usernameRef = useRef(username);

  useEffect(() => {
    usernameRef.current = username;
  }, [username]);

  // ---- Yjs Doc + Awareness lifecycle (tied to documentId) ----
  useEffect(() => {
    const yDocInstance = new Y.Doc();
    const awarenessInstance = new Awareness(yDocInstance);
    yDocRef.current = yDocInstance;
    awarenessRef.current = awarenessInstance;
    setYDoc(yDocInstance);
    setAwareness(awarenessInstance);

    return () => {
      if (retryTimerRef.current !== null) {
        window.clearTimeout(retryTimerRef.current);
        retryTimerRef.current = null;
      }
      providerRef.current?.destroy();
      providerRef.current = null;
      setProvider(null);
      awarenessInstance.destroy();
      awarenessRef.current = null;
      setAwareness(null);
      yDocInstance.destroy();
      yDocRef.current = null;
      setYDoc(null);
    };
  }, [documentId]);

  // ---- WebSocket connection (tied to documentId) ----
  useEffect(() => {
    const yDocInstance = yDocRef.current;
    const awarenessInstance = awarenessRef.current;
    if (!enabled || !yDocInstance || !awarenessInstance) return;

    let cancelled = false;
    const connectionId = ++connectionIdRef.current;
    const isCurrentConnection = () => !cancelled && connectionIdRef.current === connectionId;

    const clearRetryTimer = () => {
      if (retryTimerRef.current !== null) {
        window.clearTimeout(retryTimerRef.current);
        retryTimerRef.current = null;
      }
    };

    const emitUsers = () => {
      if (!awarenessInstance) return;
      setUsers(buildCollabUsers(awarenessInstance, usernameRef.current));
    };

    const scheduleReconnect = () => {
      if (!isCurrentConnection() || retryTimerRef.current !== null) return;
      retryTimerRef.current = window.setTimeout(() => {
        retryTimerRef.current = null;
        if (isCurrentConnection()) connect();
      }, 3000);
    };

    const connect = () => {
      if (!isCurrentConnection()) return;
      clearRetryTimer();
      providerRef.current?.destroy();
      providerRef.current = null;
      setProvider(null);

      setStatus("connecting");

      const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
      const host = window.location.host;

      const provider = new WebsocketProvider(
        `${protocol}//${host}/api/collaboration`,
        documentId,
        yDocInstance,
        { awareness: awarenessInstance, connect: true },
      );


      if (!isCurrentConnection()) {
        provider.destroy();
        return;
      }

      providerRef.current = provider;
      setProvider(provider);

      // Set local awareness state
      const color = pickColor(usernameRef.current);
      awarenessInstance.setLocalStateField("user", { name: usernameRef.current, color });

      emitUsers();

      provider.on("status", ({ status: s }: { status: string }) => {
        if (!isCurrentConnection()) return;
        if (s === "connected") {
          clearRetryTimer();
          setStatus("connected");
        } else if (s === "connecting") {
          setStatus("connecting");
        } else {
          setStatus("disconnected");
          scheduleReconnect();
        }
      });

      provider.on("connection-close", () => {
        if (!isCurrentConnection()) return;
        setStatus("disconnected");
        scheduleReconnect();
      });

      provider.on("connection-error", () => {
        if (!isCurrentConnection()) return;
        setStatus("disconnected");
        scheduleReconnect();
      });
    };

    connect();

    return () => {
      cancelled = true;
      connectionIdRef.current++;
      clearRetryTimer();
      awarenessInstance.setLocalState(null);

      emitUsers();
      providerRef.current?.destroy();
      providerRef.current = null;
      setProvider(null);
      setStatus("disconnected");
    };
  }, [documentId, enabled]); // only depend on documentId/enabled, NOT initialContent

  // ---- Awareness → users list ----
  useEffect(() => {
    const awareness = awarenessRef.current;
    if (!awareness) return;

    const update = () => {
      setUsers(buildCollabUsers(awareness, usernameRef.current));
    };

    update();
    awareness.on("change", update);
    return () => { awareness.off("change", update); };
  }, [documentId, username]);

  return {
    yDoc,
    awareness,
    provider,
    status,
    users,
  };
}
