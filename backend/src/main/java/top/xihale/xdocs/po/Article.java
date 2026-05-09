package top.xihale.xdocs.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.xihale.xdocs.annotation.Id;
import top.xihale.xdocs.annotation.Table;

import java.time.LocalDateTime;

/**
 * 文章实体，对应 article 表
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("article")
public class Article {

    @Id
    private Integer id;
    private Integer knowledgeBaseId;
    private String title;
    private String summary;
    private String content;
    private String contentFormat;
    private Integer authorId;
    private int status;
    private String coverImage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Article(Integer knowledgeBaseId, String title, String content, Integer authorId) {
        this.knowledgeBaseId = knowledgeBaseId;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.status = 0; // DRAFT
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
}
