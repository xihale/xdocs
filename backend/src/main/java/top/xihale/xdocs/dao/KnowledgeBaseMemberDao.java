package top.xihale.xdocs.dao;

import top.xihale.xdocs.po.KnowledgeBaseMember;
import top.xihale.xdocs.util.BaseMapper;
import top.xihale.xdocs.util.SqlBuilder;

import java.util.List;
import java.util.Optional;

/**
 * 知识库成员关系数据访问层
 */
public class KnowledgeBaseMemberDao extends BaseMapper<KnowledgeBaseMember> {

    public static final KnowledgeBaseMemberDao INSTANCE = new KnowledgeBaseMemberDao();

    public int updateRole(Integer kbId, Integer userId, int role) {
        return SqlBuilder.update("UPDATE knowledge_base_member SET role=? WHERE knowledge_base_id=? AND user_id=?")
                .param(role).param(kbId).param(userId)
                .execute();
    }

    public int updateInviteStatus(Integer kbId, Integer userId, int inviteStatus) {
        return SqlBuilder.update("UPDATE knowledge_base_member SET invite_status=? WHERE knowledge_base_id=? AND user_id=?")
                .param(inviteStatus).param(kbId).param(userId)
                .execute();
    }

    public int delete(Integer kbId, Integer userId) {
        return SqlBuilder.update("DELETE FROM knowledge_base_member WHERE knowledge_base_id=? AND user_id=?")
                .param(kbId).param(userId)
                .execute();
    }

    public int deleteByKbId(Integer kbId) {
        return SqlBuilder.update("DELETE FROM knowledge_base_member WHERE knowledge_base_id=?")
                .param(kbId)
                .execute();
    }

    public Optional<KnowledgeBaseMember> findByKbIdAndUserId(Integer kbId, Integer userId) {
        return findOne("knowledge_base_id=? AND user_id=?", kbId, userId);
    }

    public List<KnowledgeBaseMember> findByKbId(Integer kbId) {
        return findList("knowledge_base_id=?", kbId);
    }

    public List<KnowledgeBaseMember> findByUserId(Integer userId) {
        return findList("user_id=?", userId);
    }

    public List<KnowledgeBaseMember> findPendingByUserId(Integer userId) {
        return findList("user_id=? AND invite_status=0", userId);
    }
}
