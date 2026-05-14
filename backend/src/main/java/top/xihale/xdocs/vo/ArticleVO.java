package top.xihale.xdocs.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文章视图对象，附带作者信息
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArticleVO {
    private Integer id;
    private Integer knowledgeBaseId;
    private String title;
    private String summary;
    private String content;
    private String contentFormat;
    private Integer authorId;
    private String authorName;
    private String authorAvatar;
    private int status;
    private String coverImage;
    private String knowledgeBaseName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 点赞数 */
    private Integer likeCount;
    /** 当前用户是否已点赞 */
    private Boolean liked;
    /** 当前用户是否已收藏 */
    private Boolean favorited;
    /** 当前用户是否可编辑 */
    private Boolean canEdit;
    /** 所属 TEAM 名称（知识库归属 TEAM 时） */
    private String teamName;
    /** 所属 TEAM ID */
    private Integer teamId;
}
