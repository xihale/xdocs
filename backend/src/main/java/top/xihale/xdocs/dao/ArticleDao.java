package top.xihale.xdocs.dao;

import top.xihale.xdocs.po.Article;
import top.xihale.xdocs.util.BaseMapper;
import top.xihale.xdocs.util.Db;

import java.util.List;
import java.util.Optional;

/**
 * 文章数据访问层
 */
public class ArticleDao {

    private static final BaseMapper<Article> MAPPER = new BaseMapper<>(Article.class);

    private static final String PUBLIC_COLUMNS =
            "a.id, a.knowledge_base_id, a.title, a.summary, a.content, a.content_format, a.author_id, a.status, a.cover_image, a.create_time, a.update_time";

    private static final String SQL_PUBLIC = "SELECT %s FROM article a JOIN knowledge_base kb ON a.knowledge_base_id = kb.id WHERE kb.visibility = 1 AND a.status = 1 ORDER BY a.create_time DESC LIMIT :limit OFFSET :offset".formatted(PUBLIC_COLUMNS);
    private static final String SQL_PUBLIC_BY_LIKES = "SELECT %s FROM article a JOIN knowledge_base kb ON a.knowledge_base_id = kb.id LEFT JOIN article_like al ON a.id = al.article_id WHERE kb.visibility = 1 AND a.status = 1 GROUP BY a.id ORDER BY COUNT(al.id) DESC, a.create_time DESC LIMIT :limit OFFSET :offset".formatted(PUBLIC_COLUMNS);
    private static final String SQL_SEARCH_PUBLIC = "SELECT %s FROM article a JOIN knowledge_base kb ON a.knowledge_base_id = kb.id WHERE kb.visibility = 1 AND a.status = 1 AND (a.title LIKE :pattern OR a.content LIKE :pattern) ORDER BY a.create_time DESC LIMIT :limit OFFSET :offset".formatted(PUBLIC_COLUMNS);

    public static void insert(Article article) { MAPPER.insert(article); }
    public static int update(Article article) { return MAPPER.update(article); }
    public static int deleteById(Object id) { return MAPPER.deleteById(id); }
    public static Optional<Article> findById(Object id) { return MAPPER.findById(id); }
    public static List<Article> findAll() { return MAPPER.findAll(); }

    public static List<Article> findByKnowledgeBaseId(Integer kbId) {
        return MAPPER.findList("knowledge_base_id = ?", kbId);
    }

    public static List<Article> findPublicArticles(int offset, int limit) {
        return Db.sql(SQL_PUBLIC)
                .param("limit", limit).param("offset", offset)
                .query(MAPPER.mapper())
                .list();
    }

    public static List<Article> findByAuthorId(Integer authorId) {
        return MAPPER.findList("author_id = ?", authorId);
    }

    public static List<Article> findPublicArticlesOrderByLikes(int offset, int limit) {
        return Db.sql(SQL_PUBLIC_BY_LIKES)
                .param("limit", limit).param("offset", offset)
                .query(MAPPER.mapper())
                .list();
    }

    public static List<Article> searchPublic(String keyword, int offset, int limit) {
        return Db.sql(SQL_SEARCH_PUBLIC)
                .param("pattern", "%" + keyword + "%")
                .param("limit", limit).param("offset", offset)
                .query(MAPPER.mapper())
                .list();
    }
}
