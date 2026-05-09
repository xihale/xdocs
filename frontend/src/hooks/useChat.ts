import { useEffect, useRef, useState, useCallback } from "react";
import { authApi } from "../api";
import { useChatStore, getChatMessages, saveChatMessages } from "../stores/chat";

// ==================== WebSocket 消息类型 ====================

interface SystemMessage {
  type: "system";
  content: string;
}

interface OnlineMessage {
  type: "online";
  users: { userId: number; nickname: string; avatarUrl: string | null }[];
}

interface ChatWsMessage {
  type: "chat";
  id?: number;
  articleId: number;
  senderId: number;
  senderName: string;
  senderAvatar: string | null;
  content: string;
  createTime: string;
}

type WsMessage = ChatWsMessage | SystemMessage | OnlineMessage;

// ==================== 运行时收窄工具 ====================

function isObject(v: unknown): v is Record<string, unknown> {
  return typeof v === "object" && v !== null && !Array.isArray(v);
}

function parseWsMessage(raw: unknown): WsMessage | null {
  if (!isObject(raw)) return null;
  const msg = raw as Record<string, unknown>;

  switch (msg.type) {
    case "chat": {
      if (typeof msg.articleId !== "number" || typeof msg.senderId !== "number" ||
          typeof msg.senderName !== "string" || typeof msg.content !== "string" ||
          typeof msg.createTime !== "string") {
        return null;
      }
      return {
        type: "chat",
        id: typeof msg.id === "number" ? msg.id : undefined,
        articleId: msg.articleId,
        senderId: msg.senderId,
        senderName: msg.senderName,
        senderAvatar: typeof msg.senderAvatar === "string" ? msg.senderAvatar : null,
        content: msg.content,
        createTime: msg.createTime,
      };
    }
    case "system": {
      if (typeof msg.content !== "string") return null;
      return { type: "system", content: msg.content };
    }
    case "online": {
      if (!Array.isArray(msg.users)) return null;
      const users = msg.users.map((u: unknown) => {
        if (!isObject(u)) return null;
        const user = u as Record<string, unknown>;
        return {
          userId: typeof user.userId === "number" ? user.userId : 0,
          nickname: typeof user.nickname === "string" ? user.nickname : "",
          avatarUrl: typeof user.avatarUrl === "string" ? user.avatarUrl : null,
        };
      }).filter((u): u is NonNullable<typeof u> => u !== null);
      return { type: "online", users };
    }
    default:
      return null;
  }
}

// ==================== UI 消息类型 ====================

export interface ChatMsg {
  id: string;
  type: "chat" | "system";
  content: string;
  senderId?: number;
  senderName?: string | null;
  senderAvatar?: string | null;
  createTime?: string;
}

export interface UseChatReturn {
  messages: ChatMsg[];
  onlineUsers: { userId: number; nickname: string; avatarUrl: string | null }[];
  send: (content: string) => void;
  connected: boolean;
}

/** Generate a stable client-side message ID for dedup and React keys */
function msgId(type: string, senderId: number | undefined, content: string, time: string): string {
  return `${type}:${senderId ?? "sys"}:${content.slice(0, 32)}:${time}`;
}

/**
 * 文档聊天 WebSocket Hook
 *
 * - articleId=0 时不连接（chat 关闭状态）
 * - 自动重连（3s 间隔），卸载后停止
 * - 消息去重（基于 id 或 senderId+content+time）
 * - 重连期间的消息队列，连接恢复后自动发送
 */
export function useChat(articleId: number): UseChatReturn {
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimerRef = useRef<number | null>(null);
  const activeRef = useRef(false);
  const connectionIdRef = useRef(0);
  const pendingRef = useRef<string[]>([]);
  const seenIdsRef = useRef(new Set<string>());

  const [messages, setMessages] = useState<ChatMsg[]>(() => articleId ? getChatMessages(articleId) : []);
  const [onlineUsers, setOnlineUsers] = useState<OnlineMessage["users"]>([]);
  const [connected, setConnected] = useState(false);
  const prevCountRef = useRef(messages.length);

  // Reset local chat state when target article changes.
  useEffect(() => {
    const saved = articleId ? getChatMessages(articleId) : [];
    setMessages(saved);
    setOnlineUsers([]);
    setConnected(false);
    prevCountRef.current = saved.length;
    seenIdsRef.current = new Set(saved.map((m) => m.id));
  }, [articleId]);

  // Persist messages to localStorage (debounced, only when count changes)
  useEffect(() => {
    if (!articleId || messages.length === 0) return;
    if (messages.length === prevCountRef.current) return;
    prevCountRef.current = messages.length;
    const timer = setTimeout(() => {
      saveChatMessages(articleId, messages);
    }, 500);
    return () => clearTimeout(timer);
  }, [articleId, messages]);

  useEffect(() => {
    const connectionId = ++connectionIdRef.current;

    if (!articleId) {
      activeRef.current = false;
      setConnected(false);
      setOnlineUsers([]);
      return;
    }

    activeRef.current = true;

    const isCurrentConnection = () => activeRef.current && connectionIdRef.current === connectionId;

    const clearReconnectTimer = () => {
      if (reconnectTimerRef.current !== null) {
        window.clearTimeout(reconnectTimerRef.current);
        reconnectTimerRef.current = null;
      }
    };

    const scheduleReconnect = () => {
      if (!isCurrentConnection() || reconnectTimerRef.current !== null) return;
      reconnectTimerRef.current = window.setTimeout(() => {
        reconnectTimerRef.current = null;
        if (isCurrentConnection()) connect();
      }, 3000);
    };

    const flushPending = () => {
      const ws = wsRef.current;
      if (!ws || ws.readyState !== WebSocket.OPEN) return;
      while (pendingRef.current.length > 0) {
        const msg = pendingRef.current.shift()!;
        ws.send(msg);
      }
    };

    const connect = async () => {
      if (!isCurrentConnection()) return;
      clearReconnectTimer();

      const existing = wsRef.current;
      if (existing && (existing.readyState === WebSocket.CONNECTING || existing.readyState === WebSocket.OPEN)) {
        return;
      }

      // Small delay to avoid connect/disconnect thrash during navigation
      await new Promise<void>((r) => { setTimeout(r, 200); });
      if (!isCurrentConnection()) return;

      try {
        const { token } = await authApi.wsToken();
        if (!isCurrentConnection()) return;

        const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
        const host = window.location.host;
        const url = `${protocol}//${host}/api/chat/ws/${articleId}?token=${encodeURIComponent(token)}`;

        const ws = new WebSocket(url);
        wsRef.current = ws;

        ws.onopen = () => {
          if (!isCurrentConnection() || wsRef.current !== ws) return;
          setConnected(true);
          flushPending();
        };

        ws.onmessage = (event) => {
          if (!isCurrentConnection() || wsRef.current !== ws) return;
          try {
            const raw: unknown = JSON.parse(event.data);
            const msg = parseWsMessage(raw);
            if (!msg) return;

            if (msg.type === "chat") {
              const time = msg.createTime ?? new Date().toISOString();
              const id = msg.id != null
                ? `chat:${msg.id}`
                : msgId("chat", msg.senderId, msg.content, time);

              // Dedup
              if (seenIdsRef.current.has(id)) return;
              seenIdsRef.current.add(id);

              const cm: ChatMsg = {
                id,
                type: "chat",
                content: msg.content,
                senderId: msg.senderId,
                senderName: msg.senderName,
                senderAvatar: msg.senderAvatar ?? null,
                createTime: time,
              };
              setMessages((prev) => [...prev, cm]);
            } else if (msg.type === "system") {
              const id = msgId("system", undefined, msg.content, Date.now().toString());
              if (seenIdsRef.current.has(id)) return;
              seenIdsRef.current.add(id);
              setMessages((prev) => [...prev, { id, type: "system", content: msg.content }]);
            } else if (msg.type === "online") {
              setOnlineUsers(msg.users);
            }
          } catch {
            // ignore malformed message
          }
        };

        ws.onclose = () => {
          if (wsRef.current === ws) {
            wsRef.current = null;
          }
          if (!isCurrentConnection()) return;
          setConnected(false);
          scheduleReconnect();
        };

        ws.onerror = () => {
          if (ws.readyState !== WebSocket.CLOSING && ws.readyState !== WebSocket.CLOSED) {
            ws.close();
          }
        };
      } catch {
        if (!isCurrentConnection()) return;
        scheduleReconnect();
      }
    };

    connect();

    const seenIds = seenIdsRef.current;
    return () => {
      activeRef.current = false;
      connectionIdRef.current++;
      clearReconnectTimer();
      pendingRef.current = [];
      seenIds.clear();
      const ws = wsRef.current;
      if (ws) {
        ws.onopen = null;
        ws.onmessage = null;
        ws.onclose = null;
        ws.onerror = null;
        wsRef.current = null;
        ws.close();
      }
    };
  }, [articleId]);

  const send = useCallback((content: string) => {
    const payload = JSON.stringify({ type: "chat", content });
    const ws = wsRef.current;
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(payload);
    } else {
      // Queue for send after reconnection
      pendingRef.current.push(payload);
    }
  }, []);

  return { messages, onlineUsers, send, connected };
}
