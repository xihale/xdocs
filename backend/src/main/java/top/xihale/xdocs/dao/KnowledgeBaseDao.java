package top.xihale.xdocs.dao;

import top.xihale.xdocs.po.KnowledgeBase;
import top.xihale.xdocs.util.BaseMapper;

import java.util.List;

/**
 * 知识库数据访问层
 */
public class KnowledgeBaseDao extends BaseMapper<KnowledgeBase> {

    public static final KnowledgeBaseDao INSTANCE = new KnowledgeBaseDao();

    public List<KnowledgeBase> findByOwnerId(int ownerType, Integer ownerId) {
        return findList("owner_type = ? AND owner_id = ?", ownerType, ownerId);
    }

    public List<KnowledgeBase> findPublicKnowledgeBases() {
        return findList("visibility = 1");
    }

    public List<KnowledgeBase> findPublicByName(String keyword) {
        return findList("visibility = 1 AND name LIKE ? LIMIT 20", "%" + keyword + "%");
    }
}
