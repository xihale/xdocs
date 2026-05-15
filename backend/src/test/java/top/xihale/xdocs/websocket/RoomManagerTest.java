package top.xihale.xdocs.websocket;

import jakarta.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoomManagerTest {

    private RoomManager manager;

    @BeforeEach
    void setUp() {
        manager = RoomManager.getInstance();
        // Clear state
        try {
            var field = RoomManager.class.getDeclaredField("rooms");
            field.setAccessible(true);
            ((java.util.concurrent.ConcurrentHashMap<?, ?>) field.get(manager)).clear();
            field = RoomManager.class.getDeclaredField("activeUserSessions");
            field.setAccessible(true);
            ((java.util.concurrent.ConcurrentHashMap<?, ?>) field.get(manager)).clear();
        } catch (Exception ignored) {}
    }

    @Test void join_and_getRoomSize() {
        Session s = mock(Session.class);
        when(s.getId()).thenReturn("s1");
        when(s.isOpen()).thenReturn(true);

        manager.join("room1", s);
        assertEquals(1, manager.getRoomSize("room1"));
    }

    @Test void hasRoom() {
        Session s = mock(Session.class);
        when(s.getId()).thenReturn("s1");
        when(s.isOpen()).thenReturn(true);

        assertFalse(manager.hasRoom("room1"));
        manager.join("room1", s);
        assertTrue(manager.hasRoom("room1"));
    }

    @Test void leave_autoCleans() {
        Session s = mock(Session.class);
        when(s.getId()).thenReturn("s1");
        when(s.isOpen()).thenReturn(true);
        manager.join("room1", s);
        manager.leave("room1", s);
        assertEquals(0, manager.getRoomSize("room1"));
        assertFalse(manager.hasRoom("room1"));
    }

    @Test void bindActiveSession_new() {
        Session s = mock(Session.class);
        when(s.getId()).thenReturn("s1");
        when(s.isOpen()).thenReturn(true);
        manager.join("room1", s);

        assertNull(manager.bindActiveSession("room1", 1, s));
    }

    @Test void bindActiveSession_replaces() {
        Session s1 = mock(Session.class);
        when(s1.getId()).thenReturn("s1");
        when(s1.isOpen()).thenReturn(true);
        Session s2 = mock(Session.class);
        when(s2.getId()).thenReturn("s2");
        when(s2.isOpen()).thenReturn(true);

        manager.join("room1", s1);
        manager.bindActiveSession("room1", 1, s1);
        manager.join("room1", s2);

        Session prev = manager.bindActiveSession("room1", 1, s2);
        assertNotNull(prev);
        assertEquals("s1", prev.getId());
    }

    @Test void broadcastText_excludesSender() throws IOException {
        Session s1 = mock(Session.class);
        when(s1.getId()).thenReturn("s1");
        when(s1.isOpen()).thenReturn(true);
        Session s2 = mock(Session.class);
        when(s2.getId()).thenReturn("s2");
        when(s2.isOpen()).thenReturn(true);
        var remote2 = mock(jakarta.websocket.RemoteEndpoint.Basic.class);
        when(s2.getBasicRemote()).thenReturn(remote2);

        manager.join("room1", s1);
        manager.join("room1", s2);
        manager.broadcastText("room1", "hello", s1);

        verify(remote2).sendText("hello");
        verify(s1, never()).getBasicRemote();
    }

    @Test void broadcastBinary_excludesSender() throws IOException {
        Session s1 = mock(Session.class);
        when(s1.getId()).thenReturn("s1");
        when(s1.isOpen()).thenReturn(true);
        Session s2 = mock(Session.class);
        when(s2.getId()).thenReturn("s2");
        when(s2.isOpen()).thenReturn(true);
        var remote2 = mock(jakarta.websocket.RemoteEndpoint.Basic.class);
        when(s2.getBasicRemote()).thenReturn(remote2);

        manager.join("room1", s1);
        manager.join("room1", s2);
        byte[] data = {0, 1, 2};
        manager.broadcastBinary("room1", data, s1);

        verify(remote2).sendBinary(any(ByteBuffer.class));
    }

    @Test void multipleRooms_isolation() {
        Session s1 = mock(Session.class);
        when(s1.getId()).thenReturn("s1");
        when(s1.isOpen()).thenReturn(true);
        Session s2 = mock(Session.class);
        when(s2.getId()).thenReturn("s2");
        when(s2.isOpen()).thenReturn(true);

        manager.join("room1", s1);
        manager.join("room2", s2);

        assertEquals(1, manager.getRoomSize("room1"));
        assertEquals(1, manager.getRoomSize("room2"));
    }
}
