package top.xihale.xdocs.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 通知视图对象，附带发送者信息
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationVO {
    private Integer id;
    private Integer type;
    private String title;
    private String content;
    private String link;
    private boolean isRead;
    private LocalDateTime createTime;
    /** 发送者信息（可选） */
    private Integer senderId;
    private String senderName;
    private String senderAvatar;
}
