package top.xihale.xdocs.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识库视图对象，附带创建者信息
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeBaseVO {
    private Integer id;
    private String name;
    private String description;
    private int visibility;
    private int ownerType;
    private Integer ownerId;
    private String ownerName;
    private Integer creatorId;
    private String creatorName;
    private int articleCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
