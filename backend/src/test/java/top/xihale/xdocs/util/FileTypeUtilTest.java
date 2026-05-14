package top.xihale.xdocs.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileTypeUtilTest {

    @Test
    void detectMimeType_jpegBytes() {
        // JPEG magic number: FF D8 FF
        byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        assertEquals("image/jpeg", FileTypeUtil.detectMimeType(jpegBytes));
    }

    @Test
    void detectMimeType_pngBytes() {
        // PNG magic number: 89 50 4E 47
        byte[] pngBytes = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertEquals("image/png", FileTypeUtil.detectMimeType(pngBytes));
    }

    @Test
    void isAllowed_jpegMatch() {
        assertTrue(FileTypeUtil.isAllowed("image/jpeg", "jpg"));
        assertTrue(FileTypeUtil.isAllowed("image/jpeg", "jpeg"));
    }

    @Test
    void isAllowed_pngMatch() {
        assertTrue(FileTypeUtil.isAllowed("image/png", "png"));
    }

    @Test
    void isAllowed_mismatch() {
        assertFalse(FileTypeUtil.isAllowed("image/png", "jpg"));
    }

    @Test
    void isAllowed_unknownExtension() {
        assertFalse(FileTypeUtil.isAllowed("image/jpeg", "exe"));
    }

    @Test
    void isAllowed_caseInsensitive() {
        assertTrue(FileTypeUtil.isAllowed("image/jpeg", "JPG"));
    }

    @Test
    void isAllowed_svgVariants() {
        assertTrue(FileTypeUtil.isAllowed("image/svg+xml", "svg"));
        assertTrue(FileTypeUtil.isAllowed("text/xml", "svg"));
    }
}
