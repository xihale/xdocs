package top.xihale.xdocs.dao;

import org.junit.jupiter.api.Test;
import top.xihale.xdocs.po.*;

import static org.junit.jupiter.api.Assertions.*;

class NotificationDaoTest extends BaseDaoTest {

    private User createUser() {
        User u = new User("u", "p", "u@t.com");
        UserDao.insert(u);
        return u;
    }

    @Test void insert_and_findById() {
        User u = createUser();
        Notification n = new Notification(u.getId(), 0, "Title", "Content", "/link", null);
        NotificationDao.insert(n);
        assertNotNull(n.getId());
        assertTrue(NotificationDao.findById(n.getId()).isPresent());
    }

    @Test void findByUserId_paginated() {
        User u = createUser();
        NotificationDao.insert(new Notification(u.getId(), 0, "N1", "c", null, null));
        NotificationDao.insert(new Notification(u.getId(), 0, "N2", "c", null, null));

        var list = NotificationDao.findByUserId(u.getId(), 0, 10);
        assertTrue(list.size() >= 2);
    }

    @Test void countUnread() {
        User u = createUser();
        NotificationDao.insert(new Notification(u.getId(), 0, "T", "c", null, null));
        assertEquals(1, NotificationDao.countUnread(u.getId()));
    }

    @Test void markRead() {
        User u = createUser();
        Notification n = new Notification(u.getId(), 0, "T", "c", null, null);
        NotificationDao.insert(n);
        NotificationDao.markRead(n.getId(), u.getId());
        Notification updated = NotificationDao.findById(n.getId()).get();
        assertTrue(updated.isRead());
    }

    @Test void markAllRead() {
        User u = createUser();
        NotificationDao.insert(new Notification(u.getId(), 0, "T1", "c", null, null));
        NotificationDao.insert(new Notification(u.getId(), 0, "T2", "c", null, null));
        NotificationDao.markAllRead(u.getId());
        assertEquals(0, NotificationDao.countUnread(u.getId()));
    }

    @Test void delete() {
        User u = createUser();
        Notification n = new Notification(u.getId(), 0, "T", "c", null, null);
        NotificationDao.insert(n);
        NotificationDao.delete(n.getId(), u.getId());
        assertTrue(NotificationDao.findById(n.getId()).isEmpty());
    }
}
