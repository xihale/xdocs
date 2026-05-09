import { create } from "zustand";
import type { ChatMsg } from "../hooks/useChat";

/** Per-article persisted chat data */
interface ArticleChatRecord {
  messages: ChatMsg[];
  savedAt: number;
}

// ==================== Zustand: only openMap ====================

interface ChatState {
  openMap: Record<number, boolean>;
  setOpen: (articleId: number, open: boolean) => void;
}

const OPEN_KEY = "clouddoc-chat-open";

function loadOpenMap(): Record<number, boolean> {
  try {
    const raw = localStorage.getItem(OPEN_KEY);
    if (!raw) return {};
    return JSON.parse(raw) as Record<number, boolean>;
  } catch {
    return {};
  }
}

export const useChatStore = create<ChatState>((set) => ({
  openMap: loadOpenMap(),

  setOpen: (articleId, open) => {
    set((state) => {
      const next = { ...state.openMap, [articleId]: open };
      try { localStorage.setItem(OPEN_KEY, JSON.stringify(next)); } catch {}
      return { openMap: next };
    });
  },
}));

// ==================== In-memory cached localStorage helpers ====================

const RECORDS_KEY = "clouddoc-chat-records";

let _cache: Record<number, ArticleChatRecord> | null = null;

function getAllRecords(): Record<number, ArticleChatRecord> {
  if (_cache) return _cache;
  try {
    const raw = localStorage.getItem(RECORDS_KEY);
    if (!raw) { _cache = {}; return _cache; }
    const parsed = JSON.parse(raw) as Record<string, ArticleChatRecord>;
    const result: Record<number, ArticleChatRecord> = {};
    for (const [k, v] of Object.entries(parsed)) {
      result[Number(k)] = v;
    }
    _cache = result;
    return _cache;
  } catch {
    _cache = {};
    return _cache;
  }
}

function flushToStorage() {
  if (!_cache) return;
  try {
    localStorage.setItem(RECORDS_KEY, JSON.stringify(_cache));
  } catch {}
}

/** Read persisted messages for an article (no subscription, cached) */
export function getChatMessages(articleId: number): ChatMsg[] {
  return getAllRecords()[articleId]?.messages ?? [];
}

/** Read persisted savedAt for an article (no subscription, cached) */
export function getChatSavedAt(articleId: number): number | null {
  return getAllRecords()[articleId]?.savedAt ?? null;
}

/** Save messages for an article (in-memory cache + localStorage) */
export function saveChatMessages(articleId: number, messages: ChatMsg[]) {
  const all = getAllRecords();
  all[articleId] = { messages, savedAt: Date.now() };
  flushToStorage();
}

/** Clear persisted messages for an article */
export function clearChatMessages(articleId: number) {
  const all = getAllRecords();
  delete all[articleId];
  flushToStorage();
}
