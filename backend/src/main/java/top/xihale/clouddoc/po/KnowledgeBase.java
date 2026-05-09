package top.xihale.clouddoc.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.xihale.clouddoc.annotation.Id;
import top.xihale.clouddoc.annotation.Table;

import java.time.LocalDateTime;

/**
 * 知识库实体，对应 knowledge_base 表
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("knowledge_base")
public class KnowledgeBase {

    @Id
    private Integer id;
    private String name;
    private String description;
    private int visibility;
    private int ownerType;
    private Integer ownerId;
    private Integer creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public KnowledgeBase(String name, String description, int visibility, int ownerType, Integer ownerId, Integer creatorId) {
        this.name = name;
        this.description = description;
        this.visibility = visibility;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.creatorId = creatorId;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
}
