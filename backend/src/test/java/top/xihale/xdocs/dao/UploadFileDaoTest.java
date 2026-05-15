package top.xihale.xdocs.dao;

import org.junit.jupiter.api.Test;
import top.xihale.xdocs.po.*;

import static org.junit.jupiter.api.Assertions.*;

class UploadFileDaoTest extends BaseDaoTest {

    @Test void insert_and_findByBizTypeAndBizId() {
        User u = new User("u", "p", "u@t.com");
        UserDao.insert(u);
        UploadFile file = new UploadFile("avatar", 1, "photo.png", "/uploads/photo.png", 1024L, u.getId());
        UploadFileDao.insert(file);
        assertNotNull(file.getId());

        var files = UploadFileDao.findByBizTypeAndBizId("avatar", 1);
        assertEquals(1, files.size());
        assertEquals("photo.png", files.get(0).getFileName());
    }

    @Test void findByBizTypeAndBizId_noResults() {
        assertTrue(UploadFileDao.findByBizTypeAndBizId("nonexistent", 999).isEmpty());
    }
}
