package top.xihale.xdocs.dao;

import top.xihale.xdocs.po.KnowledgeBaseMember;
import top.xihale.xdocs.util.BaseMapper;
import top.xihale.xdocs.util.Db;

import java.util.List;
import java.util.Optional;

/**
 * 知识库成员关系数据访问层
 */
public class KnowledgeBaseMemberDao {

    private static final BaseMapper<KnowledgeBaseMember> MAPPER = new BaseMapper<>(KnowledgeBaseMember.class);

    public static void insert(KnowledgeBaseMember member) { MAPPER.insert(member); }

    public static int updateRole(Integer kbId, Integer userId, int role) {
        return Db.sql("UPDATE knowledge_base_member SET role = :role WHERE knowledge_base_id = :kbId AND user_id = :userId")
                .param("role", role).param("kbId", kbId).param("userId", userId)
                .execute();
    }

    public static int updateInviteStatus(Integer kbId, Integer userId, int inviteStatus) {
        return Db.sql("UPDATE knowledge_base_member SET invite_status = :inviteStatus WHERE knowledge_base_id = :kbId AND user_id = :userId")
                .param("inviteStatus", inviteStatus).param("kbId", kbId).param("userId", userId)
                .execute();
    }

    public static int delete(Integer kbId, Integer userId) {
        return Db.sql("DELETE FROM knowledge_base_member WHERE knowledge_base_id = :kbId AND user_id = :userId")
                .param("kbId", kbId).param("userId", userId)
                .execute();
    }

    public static int deleteByKbId(Integer kbId) {
        return Db.sql("DELETE FROM knowledge_base_member WHERE knowledge_base_id = :kbId")
                .param("kbId", kbId)
                .execute();
    }

    public static Optional<KnowledgeBaseMember> findByKbIdAndUserId(Integer kbId, Integer userId) {
        return MAPPER.findOne("knowledge_base_id = ? AND user_id = ?", kbId, userId);
    }

    public static List<KnowledgeBaseMember> findByKbId(Integer kbId) {
        return MAPPER.findList("knowledge_base_id = ?", kbId);
    }

    public static List<KnowledgeBaseMember> findByUserId(Integer userId) {
        return MAPPER.findList("user_id = ?", userId);
    }

    public static List<KnowledgeBaseMember> findPendingByUserId(Integer userId) {
        return MAPPER.findList("user_id = ? AND invite_status = 0", userId);
    }
}
