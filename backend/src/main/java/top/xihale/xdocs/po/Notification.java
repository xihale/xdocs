package top.xihale.xdocs.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.xihale.xdocs.annotation.Id;
import top.xihale.xdocs.annotation.Table;

import java.time.LocalDateTime;

/**
 * 通知实体，对应 notification 表
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("notification")
public class Notification {

    @Id
    private Integer id;
    private Integer userId;
    private int type;
    private String title;
    private String content;
    private String link;
    private Integer senderId;
    private int isRead;
    private LocalDateTime createTime;

    public Notification(Integer userId, int type, String title, String content, String link, Integer senderId) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.link = link;
        this.senderId = senderId;
        this.isRead = 0;
        this.createTime = LocalDateTime.now();
    }
}
