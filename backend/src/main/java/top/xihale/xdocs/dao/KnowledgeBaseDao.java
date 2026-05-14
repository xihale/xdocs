package top.xihale.xdocs.dao;

import top.xihale.xdocs.po.KnowledgeBase;
import top.xihale.xdocs.util.BaseMapper;

import java.util.List;
import java.util.Optional;

/**
 * 知识库数据访问层
 */
public class KnowledgeBaseDao {

    private static final BaseMapper<KnowledgeBase> MAPPER = new BaseMapper<>(KnowledgeBase.class);

    public static void insert(KnowledgeBase kb) { MAPPER.insert(kb); }
    public static int update(KnowledgeBase kb) { return MAPPER.update(kb); }
    public static int deleteById(Object id) { return MAPPER.deleteById(id); }
    public static Optional<KnowledgeBase> findById(Object id) { return MAPPER.findById(id); }
    public static List<KnowledgeBase> findAll() { return MAPPER.findAll(); }

    public static List<KnowledgeBase> findByOwnerId(int ownerType, Integer ownerId) {
        return MAPPER.findList("owner_type = ? AND owner_id = ?", ownerType, ownerId);
    }

    public static List<KnowledgeBase> findPublicKnowledgeBases() {
        return MAPPER.findList("visibility = 1");
    }

    public static List<KnowledgeBase> findPublicByName(String keyword) {
        return MAPPER.findList("visibility = 1 AND name LIKE ? LIMIT 20", "%" + keyword + "%");
    }
}
