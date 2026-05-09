package top.xihale.clouddoc.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.xihale.clouddoc.annotation.Id;
import top.xihale.clouddoc.annotation.Table;

import java.time.LocalDateTime;

/**
 * 文档聊天消息实体，对应 article_chat_message 表
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("article_chat_message")
public class ArticleChatMessage {

    @Id
    private Integer id;
    private Integer articleId;
    private Integer teamId;
    private Integer senderId;
    private int messageType;
    private String content;
    private LocalDateTime createTime;

    public ArticleChatMessage(Integer articleId, Integer teamId, Integer senderId, int messageType, String content) {
        this.articleId = articleId;
        this.teamId = teamId;
        this.senderId = senderId;
        this.messageType = messageType;
        this.content = content;
        this.createTime = LocalDateTime.now();
    }
}
