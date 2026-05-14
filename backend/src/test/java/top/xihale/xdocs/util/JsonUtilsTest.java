package top.xihale.xdocs.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsTest {

    @Test
    void toJson_simpleObject() {
        record Foo(String name, int age) {}
        String json = JsonUtils.toJson(new Foo("alice", 30));
        assertTrue(json.contains("\"name\":\"alice\""));
        assertTrue(json.contains("\"age\":30"));
    }

    @Test
    void fromJson_simpleObject() {
        record Foo(String name, int age) {}
        Foo foo = JsonUtils.fromJson("{\"name\":\"bob\",\"age\":25}", Foo.class);
        assertEquals("bob", foo.name());
        assertEquals(25, foo.age());
    }

    @Test
    void toJson_localDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2025, 1, 15, 10, 30, 0);
        String json = JsonUtils.toJson(ldt);
        assertEquals("\"2025-01-15 10:30:00\"", json);
    }

    @Test
    void fromJson_localDateTime() {
        record Holder(LocalDateTime time) {}
        Holder h = JsonUtils.fromJson("{\"time\":\"2025-06-01 12:00:00\"}", Holder.class);
        assertEquals(LocalDateTime.of(2025, 6, 1, 12, 0, 0), h.time());
    }

    @Test
    void toJson_nullField() {
        String json = JsonUtils.toJson(null);
        assertEquals("null", json);
    }

    @Test
    void roundTrip_preservesData() {
        record Data(String name, int count) {}
        Data original = new Data("test", 42);
        Data restored = JsonUtils.fromJson(JsonUtils.toJson(original), Data.class);
        assertEquals(original, restored);
    }
}
