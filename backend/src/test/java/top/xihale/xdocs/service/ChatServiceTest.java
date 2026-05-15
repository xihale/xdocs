package top.xihale.xdocs.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import top.xihale.xdocs.dao.ArticleChatMessageDao;
import top.xihale.xdocs.dao.UserDao;
import top.xihale.xdocs.po.ArticleChatMessage;
import top.xihale.xdocs.po.User;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    private MockedStatic<ArticleChatMessageDao> cmDaoMock;
    private MockedStatic<UserDao> userDaoMock;

    @BeforeEach
    void setUp() {
        cmDaoMock = mockStatic(ArticleChatMessageDao.class);
        userDaoMock = mockStatic(UserDao.class);
    }

    @AfterEach
    void tearDown() {
        userDaoMock.close();
        cmDaoMock.close();
    }

    @Test
    void sendMessage() {
        ChatService.sendMessage(1, null, 2, 0, "Hello");
        cmDaoMock.verify(() -> ArticleChatMessageDao.insert(any(ArticleChatMessage.class)));
    }

    @Test
    void getHistory() {
        ArticleChatMessage msg = new ArticleChatMessage(1, null, 2, 0, "Hi");
        cmDaoMock.when(() -> ArticleChatMessageDao.findByArticleId(1, 10)).thenReturn(List.of(msg));
        var result = ChatService.getHistory(1, 10);
        assertEquals(1, result.size());
    }

    @Test
    void getHistoryWithVO() {
        ArticleChatMessage msg = new ArticleChatMessage(1, null, 2, 0, "Hi");
        User sender = new User("u", "p", "u@t.com");
        sender.setNickname("User");
        cmDaoMock.when(() -> ArticleChatMessageDao.findByArticleId(1, 10)).thenReturn(List.of(msg));
        userDaoMock.when(() -> UserDao.findById(2)).thenReturn(Optional.of(sender));

        var result = ChatService.getHistoryWithVO(1, 10);
        assertEquals(1, result.size());
        assertEquals("Hi", result.get(0).getContent());
        assertEquals("User", result.get(0).getSenderName());
    }

    @Test
    void getHistoryWithVO_senderNotFound() {
        ArticleChatMessage msg = new ArticleChatMessage(1, null, 999, 0, "Hi");
        cmDaoMock.when(() -> ArticleChatMessageDao.findByArticleId(1, 10)).thenReturn(List.of(msg));
        userDaoMock.when(() -> UserDao.findById(999)).thenReturn(Optional.empty());

        var result = ChatService.getHistoryWithVO(1, 10);
        assertEquals("未知用户", result.get(0).getSenderName());
    }
}
