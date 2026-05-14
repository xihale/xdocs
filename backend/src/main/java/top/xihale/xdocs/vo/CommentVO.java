package top.xihale.xdocs.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 评论视图对象，附带发送者信息
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentVO {
    private Integer id;
    private Integer articleId;
    private Integer userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private Integer parentId;
    private Integer replyToId;
    private String replyToNickname;
    private String content;
    private String createTime;
}
