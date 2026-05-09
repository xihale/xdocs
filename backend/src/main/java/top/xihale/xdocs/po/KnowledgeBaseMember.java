package top.xihale.xdocs.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.xihale.xdocs.annotation.Id;
import top.xihale.xdocs.annotation.Table;

import java.time.LocalDateTime;

/**
 * 知识库成员关系实体，对应 knowledge_base_member 表
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("knowledge_base_member")
public class KnowledgeBaseMember {

    @Id
    private Integer id;
    private Integer knowledgeBaseId;
    private Integer userId;
    private int role;
    private int inviteStatus;
    private Integer inviteBy;
    private LocalDateTime joinTime;

    public KnowledgeBaseMember(Integer knowledgeBaseId, Integer userId, int role, int inviteStatus, Integer inviteBy) {
        this.knowledgeBaseId = knowledgeBaseId;
        this.userId = userId;
        this.role = role;
        this.inviteStatus = inviteStatus;
        this.inviteBy = inviteBy;
        this.joinTime = LocalDateTime.now();
    }
}
