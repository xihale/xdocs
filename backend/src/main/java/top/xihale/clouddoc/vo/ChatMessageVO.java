package top.xihale.clouddoc.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 聊天消息视图对象，附带发送者信息
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageVO {
    private Integer id;
    private Integer articleId;
    private Integer senderId;
    private String senderName;
    private String senderAvatar;
    private int messageType;
    private String content;
    private LocalDateTime createTime;
}
