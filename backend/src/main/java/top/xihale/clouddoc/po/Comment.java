package top.xihale.clouddoc.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.xihale.clouddoc.annotation.Id;
import top.xihale.clouddoc.annotation.Table;

import java.time.LocalDateTime;

/**
 * 评论实体，对应 comment 表
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table("comment")
public class Comment {
    @Id
    private Integer id;
    private int articleId;
    private int userId;
    private Integer parentId;
    private Integer replyToId;
    private String content;
    private LocalDateTime createTime;
}
