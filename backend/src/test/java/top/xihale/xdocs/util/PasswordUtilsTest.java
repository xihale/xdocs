package top.xihale.xdocs.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilsTest {

    @Test
    void hash_producesNonNullBcrypt() {
        String hash = PasswordUtils.hash("secret123");
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2"));
    }

    @Test
    void verify_correctPassword() {
        String hash = PasswordUtils.hash("mypassword");
        assertTrue(PasswordUtils.verify("mypassword", hash));
    }

    @Test
    void verify_wrongPassword() {
        String hash = PasswordUtils.hash("mypassword");
        assertFalse(PasswordUtils.verify("wrongpassword", hash));
    }

    @Test
    void hash_differentSaltsEachTime() {
        String hash1 = PasswordUtils.hash("samepassword");
        String hash2 = PasswordUtils.hash("samepassword");
        assertNotEquals(hash1, hash2); // BCrypt uses random salt
    }
}
