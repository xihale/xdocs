package top.xihale.xdocs.service;

import top.xihale.xdocs.dao.ArticleChatMessageDao;
import top.xihale.xdocs.dao.UserDao;
import top.xihale.xdocs.po.ArticleChatMessage;
import top.xihale.xdocs.po.User;
import top.xihale.xdocs.vo.ChatMessageVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天业务逻辑层
 */
public class ChatService {

    public static void sendMessage(int articleId, Integer teamId, int senderId, int messageType, String content) {
        ArticleChatMessage message = new ArticleChatMessage(articleId, teamId, senderId, messageType, content);
        ArticleChatMessageDao.insert(message);
    }

    public static List<ArticleChatMessage> getHistory(int articleId, int limit) {
        return ArticleChatMessageDao.findByArticleId(articleId, limit);
    }

    public static List<ChatMessageVO> getHistoryWithVO(int articleId, int limit) {
        List<ArticleChatMessage> messages = getHistory(articleId, limit);
        List<ChatMessageVO> voList = new ArrayList<>();
        for (ArticleChatMessage msg : messages) {
            User sender = UserDao.findById(msg.getSenderId()).orElse(null);

            voList.add(ChatMessageVO.builder()
                    .id(msg.getId())
                    .articleId(msg.getArticleId())
                    .senderId(msg.getSenderId())
                    .senderName(sender != null ? sender.getNickname() : "未知用户")
                    .senderAvatar(sender != null ? sender.getAvatarUrl() : null)
                    .messageType(msg.getMessageType())
                    .content(msg.getContent())
                    .createTime(msg.getCreateTime())
                    .build());
        }
        return voList;
    }
}
