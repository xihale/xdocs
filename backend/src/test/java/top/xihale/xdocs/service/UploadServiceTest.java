package top.xihale.xdocs.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import top.xihale.xdocs.dao.UploadFileDao;
import top.xihale.xdocs.po.UploadFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UploadServiceTest {

    private MockedStatic<UploadFileDao> daoMock;

    @BeforeEach
    void setUp() { daoMock = mockStatic(UploadFileDao.class); }
    @AfterEach
    void tearDown() { daoMock.close(); }

    @Test
    void uploadFile() {
        UploadFile result = UploadService.uploadFile("avatar", 1, "f.png", "/u/f.png", 1024L, 1);
        assertNotNull(result);
        assertEquals("f.png", result.getFileName());
        daoMock.verify(() -> UploadFileDao.insert(any(UploadFile.class)));
    }

    @Test
    void findByBizTypeAndBizId() {
        UploadFile f = new UploadFile("image", 2, "p.jpg", "/p.jpg", 512L, 1);
        daoMock.when(() -> UploadFileDao.findByBizTypeAndBizId("image", 2)).thenReturn(List.of(f));
        var result = UploadService.findByBizTypeAndBizId("image", 2);
        assertEquals(1, result.size());
        assertEquals("p.jpg", result.get(0).getFileName());
    }

    @Test
    void findByBizTypeAndBizId_empty() {
        daoMock.when(() -> UploadFileDao.findByBizTypeAndBizId("none", 0)).thenReturn(List.of());
        assertTrue(UploadService.findByBizTypeAndBizId("none", 0).isEmpty());
    }
}
