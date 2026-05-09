# Editor + Collaboration/Chat WebSocket Refactor Plan

## Objective

Identify all bugs, design issues, and maintainability problems in the frontend Editor and backend Collaboration/Chat WebSocket code, then propose an incremental refactoring plan to resolve them.

---

## Part 1: Bug & Issue Inventory

### CRITICAL — Data Loss / Protocol Incorrectness

#### BUG-1: Backend stores raw SyncStep2 message as `docState` instead of Yjs document update
- **Severity**: CRITICAL
- **Location**: `CollaborationWebSocket.java:150`
- **Description**: When a client sends SyncStep2, the backend stores the *entire message including headers* (`[0, 1, ...payload]`) as `docStates.get(docId)`. When a new user joins, this raw message is sent directly (`line 73`). But the new user has already received a SyncStep1 (`[0, 0]`). The y-websocket client will interpret the stored bytes as a new top-level message with messageType=0 (Sync), syncType=1 (SyncStep2), which happens to be correct by accident. However, the stored state **never gets updated with incremental Update messages** (syncType=2) — see BUG-2. This means the stored state becomes stale after any edit, and new users joining later receive outdated content.
- **Impact**: New users joining after edits get stale document content. Data divergence between clients.

#### BUG-2: Incremental Update messages (syncType=2) are only forwarded, never merged into stored state
- **Severity**: CRITICAL
- **Location**: `CollaborationWebSocket.java:153-165`
- **Description**: The code explicitly states "Update 消息只转发，不修改存储". Since Java has no Yjs runtime, the backend cannot merge updates. The stored `docState` only gets refreshed when a client happens to send a full SyncStep2. This means:
  1. After initial sync, every edit produces Update messages
  2. These are forwarded to existing room members but NOT stored
  3. A new user joining gets the stale SyncStep2 from when the first client connected
  4. The new user's Yjs doc will be missing all subsequent edits
- **Impact**: Data loss for users joining after edits have been made. This is the root cause of the reported "0人协作" and sync failures.

#### BUG-3: Backend sends stored state AND SyncStep1 to new user simultaneously — race condition
- **Severity**: HIGH
- **Location**: `CollaborationWebSocket.java:63-74`
- **Description**: On `onOpen`, the server sends `[0, 0]` (SyncStep1) followed immediately by the stored `docState`. The y-websocket client, upon receiving SyncStep1, will reply with its own SyncStep2 (its current state). But it also receives the stored state (which is a stale SyncStep2 from BUG-2). The client's Yjs library will try to merge both, but the timing is unpredictable. If the stored state arrives before the client processes SyncStep1, the protocol flow is violated.
- **Impact**: Unpredictable sync behavior, potential content duplication or corruption.

### HIGH — Resource Leaks & Race Conditions

#### BUG-4: `useChat` reconnection loop never stops — stale closure over `active` flag
- **Severity**: HIGH
- **Location**: `useChat.ts:160-168`
- **Description**: The `ws.onclose` handler schedules `connect()` via `setTimeout`. The `connect` function is a closure that captures the outer `active` variable. However, the `setTimeout` callback checks `if (active)` before calling `connect()`, but `connect()` itself is the *same function reference* — it doesn't re-read `active` inside the async flow. More critically, each `connect()` call creates a *new* WebSocket, and if the previous one's `onclose` fires after cleanup, it can trigger reconnection even after the effect cleanup has set `active = false`. The `setTimeout` closures capture `active` by reference, but the timing between `ws.close()` in cleanup and `onclose` firing is non-deterministic.
- **Impact**: WebSocket connections can leak or reconnect after component unmount.

#### BUG-5: `useChat` — `send` callback is stale, never updates after reconnection
- **Severity**: HIGH
- **Location**: `useChat.ts:190-193`
- **Description**: `send` is wrapped in `useCallback` with `[]` deps. It reads `wsRef.current` at call time, which is correct for getting the latest WS instance. However, the `wsRef.current` is set inside the `connect` closure which runs asynchronously. Between the effect cleanup (which sets `wsRef.current = null`) and the new `connect()` completing, `send()` will silently drop messages because `wsRef.current` is null or not yet OPEN. There's no queue or retry for messages sent during reconnection.
- **Impact**: Chat messages silently lost during reconnection windows.

#### BUG-6: Editor WebSocket effect re-runs on `initialContent` change, destroying and recreating provider
- **Severity**: HIGH
- **Location**: `Editor.tsx:277` — deps `[documentId, username, initialContent]`
- **Description**: The WebSocket connection effect depends on `initialContent`. If the parent re-renders and passes a new `initialContent` string (even with the same value, due to referential inequality), the entire WebSocket provider is destroyed and recreated. In `ArticleEditPage.tsx`, `initialContent` comes from `article.content ?? undefined`, which is a new string reference each render. Combined with the fact that `article` state is set once on load, this is currently stable, but it's fragile — any future change that causes `article` to re-set (e.g., auto-save updating `lastSavedContentRef`) could trigger this.
- **Impact**: Potential unnecessary WebSocket reconnections and content loss if `initialContent` reference changes.

#### BUG-7: Collaboration backend — `closeExistingSession` method is missing from `CollaborationWebSocket`
- **Severity**: HIGH
- **Location**: `CollaborationWebSocket.java:57` — calls `closeExistingSession(docId, userId, session)` but the method doesn't exist
- **Description**: Wait — looking more carefully, the code uses `bindActiveSession` and `closePreviousSession` (lines 237-286). The method call on line 57 is `closeExistingSession` which does NOT exist. Let me re-check... Actually, looking at the code again, the search result showed the `onOpen` at line 43-85 in the version I read. Let me re-examine. The file I read has `bindActiveSession` and `closePreviousSession` — the `closeExistingSession` appears only in the sem_search result which shows a *different version* of the file. The actual file I read (436 lines for Editor.tsx) uses `bindActiveSession`/`closePreviousSession`. So this is NOT a bug in the current version — the CollaborationWebSocket properly has `bindActiveSession` and `closePreviousSession`.
- **Update**: After re-reading, the current CollaborationWebSocket.java (374 lines) correctly uses `bindActiveSession` + `closePreviousSession`. NOT a bug.

#### BUG-8: Milkdown editor re-creates on every render due to `MilkdownProvider` placement
- **Severity**: HIGH
- **Location**: `Editor.tsx:333-343`
- **Description**: `<MilkdownProvider>` is placed inside the JSX that re-renders on every `sourceContent` state change. The `MilkdownEditorInner` component receives refs (which are stable), but the `useEditor` hook creates a new editor configuration each time the component mounts. If React strict mode or any state change causes the parent to unmount/remount this subtree, the entire Milkdown editor is destroyed and recreated, losing all collaborative state.
- **Impact**: Editor can be destroyed and recreated on mode switches or state changes, causing content flicker and potential data loss.

### MEDIUM — Logic Errors & Protocol Issues

#### BUG-9: `templateAppliedRef` prevents re-applying template even after reconnect to empty doc
- **Severity**: MEDIUM
- **Location**: `Editor.tsx:241`
- **Description**: If the WebSocket disconnects and reconnects, and the server's stored state has been lost (e.g., room was empty and `docStates` was cleaned up), the client won't re-apply the initial content because `templateAppliedRef.current` is already `true`. The doc will appear empty.
- **Impact**: Content loss scenario on reconnect after server state cleanup.

#### BUG-10: Source mode ↔ WYSIWYG mode switch uses `applyTemplate` which replaces all content
- **Severity**: MEDIUM
- **Location**: `Editor.tsx:311-317`
- **Description**: When switching from source mode back to WYSIWYG, `cs.applyTemplate(content)` is called. This replaces the entire Yjs document content. If another user was editing simultaneously, their changes will be overwritten. The mode switch is not collaborative-safe.
- **Impact**: Collaborative edits can be lost when a user switches from source to WYSIWYG mode.

#### BUG-11: `collabService.connect()` is called without waiting for WebSocket sync
- **Severity**: MEDIUM
- **Location**: `Editor.tsx:423`
- **Description**: The Milkdown collab service calls `connect()` immediately after `bindDoc`/`setAwareness`. This tells Milkdown to start rendering from the Yjs doc. But at this point, the WebSocket may not have synced yet — the Yjs doc is empty. The editor will briefly show empty content, then update when sync completes. Combined with the template application logic, this creates a complex race between sync, template apply, and editor rendering.
- **Impact**: Brief flash of empty editor on load; complex race conditions.

#### BUG-12: Chat backend — `closeExistingSession` removes session from room before calling `onClose`
- **Severity**: MEDIUM
- **Location**: `ChatWebSocket.java:200-202`
- **Description**: In `closeExistingSession`, the old session is removed from `roomSessions` and closed. But `session.close()` triggers `onClose`, which tries to remove the session from `roomSessions` again and broadcast a leave message. The `replaced` flag prevents the leave message, but the double removal is wasteful. More importantly, `sessionUserMap.remove(s.getId())` is called in `closeExistingSession` AND in `onClose`, which is harmless but indicates confused ownership.
- **Impact**: Minor — no user-visible bug, but indicates unclear lifecycle management.

#### BUG-13: Collaboration backend — awareness remove on disconnect uses extracted `yjsClientId` which may be null
- **Severity**: MEDIUM
- **Location**: `CollaborationWebSocket.java:188-195`
- **Description**: The `yjsClientId` is only set when the client sends an awareness message (`extractAndStoreClientId`). If a client connects but never sends awareness data (e.g., connection drops immediately), the `yjsClientId` will be null, and no awareness remove message is broadcast. Other clients will show a stale cursor for the disconnected user.
- **Impact**: Ghost cursors remaining after user disconnects without having sent awareness.

#### BUG-14: `handleSave` auto-save fires even when content hasn't changed
- **Severity**: MEDIUM
- **Location**: `ArticleEditPage.tsx:75-78`
- **Description**: The auto-save interval calls `handleSave()` every 5 seconds. `handleSave` checks `contentRef.current === lastSavedContentRef.current` and early-returns. This is correct but wasteful — it creates a new closure for `handleSave` on every `article` change (due to `useCallback` dep), which re-creates the interval.
- **Impact**: Unnecessary network requests; interval recreation on dependency changes.

### LOW — Maintainability & Code Quality

#### ISSUE-15: Editor component is a 436-line monolith mixing Yjs/WebSocket/Milkdown/Source-mode concerns
- **Severity**: LOW (maintainability)
- **Location**: `Editor.tsx` (entire file)
- **Description**: The `MarkdownEditor` component handles: Yjs Doc lifecycle, Awareness lifecycle, WebSocket connection/reconnection, Milkdown editor initialization, source mode switching, template application. All concerns are interleaved through shared refs. Understanding the data flow requires tracing through 5+ useEffect hooks and their dependency arrays.

#### ISSUE-16: No centralized collab state management — refs scattered across component
- **Severity**: LOW (maintainability)
- **Location**: `Editor.tsx:112-122`
- **Description**: `yDocRef`, `awarenessRef`, `wsProviderRef`, `editorRef`, `collabServiceRef`, `templateAppliedRef`, `collabRetryTimerRef` — 7 refs managing interconnected state. No single source of truth for "what is the current connection state". Cleanup logic is split across multiple useEffect returns.

#### ISSUE-17: Backend CollaborationWebSocket has no persistence — document state lost on server restart
- **Severity**: LOW (architectural limitation)
- **Location**: `CollaborationWebSocket.java:41` — `docStates` is in-memory only
- **Description**: All document states are stored in `static ConcurrentHashMap`. Server restart = all data lost. Clients will need to re-sync, and if all tabs are closed, the document state is gone. The actual source of truth is the database (saved via the save button), but real-time edits between saves are lost.
- **Impact**: Acceptable for current use case (auto-save exists), but limits reliability.

#### ISSUE-18: Chat `useChat` hook has no message deduplication
- **Severity**: LOW
- **Location**: `useChat.ts:149`
- **Description**: If the chat server broadcasts a message and the client receives it twice (e.g., during reconnection), there's no deduplication. Messages are appended with array index as key (`key={i}`), which causes React to misidentify messages on re-render.
- **Impact**: Potential duplicate messages in chat UI.

#### ISSUE-19: Chat messages use array index as React key
- **Severity**: LOW
- **Location**: `ArticleEditPage.tsx:310, 323`
- **Description**: `<div key={i}>` — using array index as key is an anti-pattern. If messages are inserted or removed, React will misidentify elements and cause incorrect rendering.
- **Impact**: Minor rendering bugs if messages are reordered.

#### ISSUE-20: Backend WebSocket endpoints use static state — untestable and not scalable
- **Severity**: LOW (architectural)
- **Location**: All backend WebSocket classes
- **Description**: All room state (`rooms`, `docStates`, `sessionUserMap`, `activeUserSessions`) is stored in `static ConcurrentHashMap`. This makes unit testing impossible (state leaks between tests) and prevents horizontal scaling (multiple server instances can't share state).
- **Impact**: Cannot run multiple instances; difficult to test.

---

## Part 2: Root Cause Analysis

The fundamental architectural problem is:

1. **Backend has no Yjs runtime** — It cannot merge incremental updates into the stored document state. This makes it a "dumb relay" that can only forward messages. The stored `docState` becomes stale immediately after the first edit.

2. **Frontend has too many interconnected concerns** — Yjs lifecycle, WebSocket lifecycle, Milkdown lifecycle, and mode-switching logic are all managed through shared refs in a single component. The dependency relationships between these lifecycles are implicit and fragile.

3. **y-websocket protocol is partially implemented** — The backend implements a custom subset of y-websocket that doesn't match the standard server behavior. The standard y-websocket server maintains a Yjs Doc and can properly respond to SyncStep1 with the correct merged state. This backend cannot.

---

## Part 3: Proposed Refactoring Approach

**Strategy**: Incremental, layered refactoring. Fix critical bugs first, then improve architecture. No full rewrite.

### Phase 1: Fix Critical Sync Bugs (Backend)

The core issue is that the backend cannot merge Yjs updates. Two approaches:

**Option A (Recommended)**: Use a proper y-websocket server (e.g., `y-websocket` Node.js server, or Hocuspocus) alongside the Java backend. The Java backend handles auth and room management; the Node.js server handles Yjs sync.

**Option B**: Keep the Java relay but fix the protocol flow:
- Don't try to store document state at all
- Act as a pure relay (forward all messages to all room members)
- Let clients handle sync via the standard y-websocket client-side sync protocol
- New users get state from existing clients, not from the server

Option B is simpler and can be done entirely within the Java codebase.

### Phase 2: Fix Frontend Lifecycle Issues

Extract the Yjs/WebSocket lifecycle into a dedicated hook or context to separate concerns from the editor UI.

### Phase 3: Improve Maintainability

Refactor the monolith editor component into smaller, focused pieces.

---

## Part 4: Detailed Implementation Steps

### Phase 1: Backend — Fix Collaboration Protocol (Option B: Pure Relay)

- [ ] **1.1 Remove `docStates` storage from `CollaborationWebSocket`**
  - Delete the `docStates` ConcurrentHashMap and all reads/writes to it
  - Rationale: The stored state is always stale (BUG-1, BUG-2). Removing it eliminates the false promise of server-side state

- [ ] **1.2 Fix `onOpen` to NOT send SyncStep1 or stored state**
  - Remove lines that send `[0, 0]` (SyncStep1) and stored state to new users
  - Instead, when a new user joins, broadcast a SyncStep1 query to existing room members
  - The y-websocket protocol handles this: existing clients will reply with SyncStep2 containing the full state, which gets forwarded to the new user
  - Rationale: Fixes BUG-3 (race condition) and BUG-1/BUG-2 (stale state)

- [ ] **1.3 Implement proper relay-only sync flow**
  - On `onOpen`: add user to room, broadcast QueryAwareness `[3]` to room (keep this)
  - On SyncStep1 (message `[0, 0, ...]` from client): forward to all OTHER room members
  - On SyncStep2 (message `[0, 1, ...]` from client): forward to all OTHER room members  
  - On Update (message `[0, 2, ...]` from client): forward to all OTHER room members
  - Rationale: Pure relay is correct because Yjs CRDT handles merge on the client side

- [ ] **1.4 Add "backfill" mechanism for new users joining empty rooms**
  - If a new user joins and no one else is in the room, the user starts with an empty doc (correct behavior — content comes from the database via `initialContent` prop)
  - If other users exist, the SyncStep1 broadcast triggers them to send SyncStep2 which gets relayed
  - Rationale: Handles the edge case of first user joining

- [ ] **1.5 Keep awareness remove on disconnect (fix BUG-13)**
  - Keep the existing awareness remove logic in `onClose`
  - Additionally, broadcast QueryAwareness `[3]` on disconnect so remaining clients re-send their awareness
  - Rationale: Ensures ghost cursors are cleaned up

- [ ] **1.6 Remove `activeUserSessions` tracking if not needed**
  - The `bindActiveSession`/`closePreviousSession` logic for same-user dedup is valuable — keep it
  - But ensure the previous session's `onClose` properly cleans up awareness
  - Rationale: Prevents duplicate connections from same user

### Phase 2: Frontend — Fix Editor Lifecycle Bugs

- [ ] **2.1 Remove `initialContent` from WebSocket effect dependencies**
  - Change `Editor.tsx:277` deps from `[documentId, username, initialContent]` to `[documentId, username]`
  - Use a ref for `initialContent` instead (already partially done via the sync flow)
  - Rationale: Fixes BUG-6 — prevents WebSocket reconnection on content reference changes

- [ ] **2.2 Fix template application to work with relay-only backend**
  - After WebSocket connects and syncs, check if Yjs doc is empty
  - If empty AND `initialContent` exists AND not yet applied, apply template
  - Use a more robust mechanism than `provider.once("sync")` — listen for the first meaningful sync
  - Reset `templateAppliedRef` only when `documentId` changes (not on reconnect)
  - Rationale: Fixes BUG-9 — template applied correctly even after reconnect

- [ ] **2.3 Fix source mode switch to be collaboration-safe**
  - When switching from source → WYSIWYG, instead of `applyTemplate` (which replaces everything), use Yjs's `Y.XmlFragment` operations to diff and apply changes
  - Alternative (simpler): Disable source mode switching during active collaboration (warn user), or accept that source mode is a single-user feature
  - Rationale: Fixes BUG-10 — prevents overwriting collaborative edits

- [ ] **2.4 Extract Yjs/WebSocket lifecycle into a custom hook `useCollabProvider`**
  - Create `useCollabProvider(documentId, username)` that returns `{ yDoc, awareness, provider, status }`
  - Handle all connection, reconnection, and cleanup in this single hook
  - The hook owns the Yjs Doc, Awareness, and WebSocket provider lifecycles
  - Rationale: Separates concerns (ISSUE-15, ISSUE-16), makes the lifecycle testable

- [ ] **2.5 Move Milkdown initialization into a separate component that receives yDoc/awareness as props**
  - Create `CollaborativeMilkdownEditor` that takes `yDoc` and `awareness` as props
  - This component only handles Milkdown setup, not WebSocket
  - Rationale: Further separation of concerns

### Phase 3: Fix Chat Issues

- [ ] **3.1 Fix `useChat` reconnection — add `active` check inside `connect` closure**
  - In `useChat.ts`, ensure the `setTimeout` in `onclose` properly checks `active`
  - Add a `reconnectTimerRef` to track and clear pending reconnection timers on cleanup
  - Rationale: Fixes BUG-4 — prevents reconnection after unmount

- [ ] **3.2 Add message queue for `send` during reconnection**
  - Buffer messages when WebSocket is not OPEN
  - Flush buffer on reconnection
  - Alternatively, show a "reconnecting" warning and disable send
  - Rationale: Fixes BUG-5 — prevents silent message loss

- [ ] **3.3 Add message deduplication in `useChat`**
  - Track message IDs or use a combination of `senderId + timestamp` as dedup key
  - Rationale: Fixes ISSUE-18

- [ ] **3.4 Use stable keys for chat messages instead of array index**
  - Use `msg.id` (from server) or generate a client-side ID
  - Rationale: Fixes ISSUE-19

### Phase 4: Maintainability Improvements

- [ ] **4.1 Create a `CollabRoom` abstraction on the backend**
  - Extract room management (join, leave, broadcast) into a reusable `RoomManager` class
  - Both `CollaborationWebSocket` and `ChatWebSocket` delegate to `RoomManager`
  - Rationale: DRY — both endpoints have nearly identical room management code

- [ ] **4.2 Replace static state with instance-based or injected state on backend**
  - Use a singleton `RoomManager` instance instead of static maps
  - This enables testing with mock room managers
  - Rationale: Addresses ISSUE-20 — testability

- [ ] **4.3 Add Javadoc protocol documentation to CollaborationWebSocket**
  - Document the exact y-websocket message flow expected
  - Include sequence diagrams for: new user join, edit broadcast, disconnect
  - Rationale: Future maintainers can understand the protocol without reading y-websocket source

- [ ] **4.4 Add TypeScript interfaces for all WebSocket message types (frontend)**
  - Create a `types/collab.ts` and `types/chat.ts` with all message interfaces
  - Currently message types are inline in components/hooks
  - Rationale: Single source of truth for protocol types

- [ ] **4.5 Add error boundaries around the editor component**
  - Wrap the Milkdown editor in a React Error Boundary
  - On crash, show a fallback with a "retry" button that re-initializes the editor
  - Rationale: Prevents full page crash on editor errors

---

## Part 5: Verification Criteria

### Critical Path Verification

- [ ] **VC-1**: Two users open the same article for editing. User A types text. User B sees the text within 2 seconds.
- [ ] **VC-2**: User A edits, then User C joins later. User C sees all of User A's edits (not stale content).
- [ ] **VC-3**: User A disconnects (network off) and reconnects. Their edits are preserved and sync resumes.
- [ ] **VC-4**: User A refreshes the page. Only ONE WebSocket connection exists on the backend (no duplicate).
- [ ] **VC-5**: All three users disconnect. Backend room is cleaned up (no memory leak in static maps).

### Chat Verification

- [ ] **VC-6**: Open chat sidebar, send a message. Other user receives it in real-time.
- [ ] **VC-7**: Close chat sidebar (unmount). No WebSocket reconnection attempts in console.
- [ ] **VC-8**: Reopen chat sidebar. Connection establishes and messages load.

### Mode Switch Verification

- [ ] **VC-9**: Switch to source mode, edit content, switch back to WYSIWYG. Content is preserved.
- [ ] **VC-10**: While User A is in source mode, User B edits in WYSIWYG. When User A switches back, User B's edits are not lost.

### Edge Cases

- [ ] **VC-11**: Server restarts while users are editing. Users reconnect and content is preserved (from last auto-save).
- [ ] **VC-12**: User with no edit permission opens the page. Editor loads read-only (or shows permission error).
- [ ] **VC-13**: Rapid tab switching (open/close edit page quickly) doesn't leak WebSocket connections.

---

## Part 6: Potential Risks and Mitigations

1. **Risk: Pure relay approach means first user in room has no one to sync from**
   - Mitigation: First user's content comes from `initialContent` prop (database). The `applyTemplate` mechanism handles this. When second user joins, the SyncStep1 broadcast triggers first user to send SyncStep2.

2. **Risk: Removing `docStates` means server can't provide state to new users when room is empty**
   - Mitigation: Empty room = no one is editing. New user gets content from database. This is the correct behavior — the database is the source of truth, not the WebSocket server.

3. **Risk: Refactoring editor component could break existing functionality**
   - Mitigation: Incremental approach — fix bugs first (Phases 1-3), then refactor (Phase 4). Each phase is independently deployable and testable.

4. **Risk: y-websocket client behavior may not work correctly with pure relay server**
   - Mitigation: The y-websocket client is designed to work with any server that correctly relays messages. The CRDT merge happens client-side. Test with VC-1 through VC-5.

5. **Risk: Source mode ↔ WYSIWYG switch during collaboration**
   - Mitigation: Phase 2 step 2.3 addresses this. If the diff-based approach is too complex, the simpler alternative is to warn users and disable mode switching when multiple users are present.

---

## Part 7: Alternative Approaches

1. **Replace Java WebSocket with Node.js y-websocket server**: Deploy a small Node.js process that runs the standard `y-websocket` server. Java backend handles auth via JWT token validation passed as a query param. This gives proper Yjs state management but adds operational complexity (two servers).

2. **Use Hocuspocus (TypeScript)**: A production-grade Yjs server with built-in persistence, auth hooks, and scaling. Same trade-off as above — adds a non-Java dependency.

3. **Embed a Yjs runtime in Java via GraalVM/JS engine**: Run a minimal JavaScript Yjs runtime inside the Java process. This would allow proper state merging but adds significant complexity and performance overhead.

4. **Accept current architecture, add periodic full-state snapshots**: Keep the current relay approach but have one client periodically send a full SyncStep2 that the server stores. This is a compromise — the stored state would be at most N seconds stale. Simpler than the above alternatives but still not fully correct.

**Recommendation**: Option B (pure relay, Phase 1) is the best balance of correctness, simplicity, and incremental improvement. It fixes the critical bugs without adding new infrastructure dependencies.
