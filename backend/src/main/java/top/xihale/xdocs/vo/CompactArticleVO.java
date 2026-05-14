package top.xihale.xdocs.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文章简要视图对象（收藏/浏览历史）
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompactArticleVO {
    private Integer id;
    private String title;
    private Integer authorId;
    private LocalDateTime updateTime;
    /** 收藏时额外附带文章创建时间 */
    private LocalDateTime createTime;
}
