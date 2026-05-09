package top.xihale.clouddoc.service;

import top.xihale.clouddoc.dao.ArticleChatMessageDao;
import top.xihale.clouddoc.dao.UserDao;
import top.xihale.clouddoc.po.ArticleChatMessage;
import top.xihale.clouddoc.po.User;
import top.xihale.clouddoc.vo.ChatMessageVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天业务逻辑层
 */
public class ChatService {

    public static void sendMessage(int articleId, Integer teamId, int senderId, int messageType, String content) {
        ArticleChatMessage message = new ArticleChatMessage(articleId, teamId, senderId, messageType, content);
        ArticleChatMessageDao.INSTANCE.insert(message);
    }

    public static List<ArticleChatMessage> getHistory(int articleId, int limit) {
        return ArticleChatMessageDao.INSTANCE.findByArticleId(articleId, limit);
    }

    public static List<ChatMessageVO> getHistoryWithVO(int articleId, int limit) {
        List<ArticleChatMessage> messages = getHistory(articleId, limit);
        List<ChatMessageVO> voList = new ArrayList<>();
        for (ArticleChatMessage msg : messages) {
            User sender = UserDao.INSTANCE.findById(msg.getSenderId()).orElse(null);

            ChatMessageVO vo = new ChatMessageVO();
            vo.setId(msg.getId());
            vo.setArticleId(msg.getArticleId());
            vo.setSenderId(msg.getSenderId());
            vo.setSenderName(sender != null ? sender.getNickname() : "未知用户");
            vo.setSenderAvatar(sender != null ? sender.getAvatarUrl() : null);
            vo.setMessageType(msg.getMessageType());
            vo.setContent(msg.getContent());
            vo.setCreateTime(msg.getCreateTime());
            voList.add(vo);
        }
        return voList;
    }
}
