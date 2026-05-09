package top.xihale.xdocs.dao;

import top.xihale.xdocs.po.Article;
import top.xihale.xdocs.util.BaseMapper;
import top.xihale.xdocs.util.SqlBuilder;

import java.util.List;

/**
 * 文章数据访问层
 */
public class ArticleDao extends BaseMapper<Article> {

    public static final ArticleDao INSTANCE = new ArticleDao();

    public List<Article> findByKnowledgeBaseId(Integer kbId) {
        return findList("knowledge_base_id = ?", kbId);
    }

    public List<Article> findPublicArticles(int offset, int limit) {
        return SqlBuilder.select("SELECT a.id, a.knowledge_base_id, a.title, a.summary, a.content, a.content_format, a.author_id, a.status, a.cover_image, a.create_time, a.update_time "
                        + "FROM article a JOIN knowledge_base kb ON a.knowledge_base_id = kb.id "
                        + "WHERE kb.visibility = 1 AND a.status = 1 ORDER BY a.create_time DESC LIMIT ? OFFSET ?")
                .param(limit).param(offset)
                .queryList(mapper());
    }

    public List<Article> findByAuthorId(Integer authorId) {
        return findList("author_id = ?", authorId);
    }

    public List<Article> findPublicArticlesOrderByLikes(int offset, int limit) {
        return SqlBuilder.select("SELECT a.id, a.knowledge_base_id, a.title, a.summary, a.content, a.content_format, a.author_id, a.status, a.cover_image, a.create_time, a.update_time "
                        + "FROM article a JOIN knowledge_base kb ON a.knowledge_base_id = kb.id "
                        + "LEFT JOIN article_like al ON a.id = al.article_id "
                        + "WHERE kb.visibility = 1 AND a.status = 1 "
                        + "GROUP BY a.id ORDER BY COUNT(al.id) DESC, a.create_time DESC LIMIT ? OFFSET ?")
                .param(limit).param(offset)
                .queryList(mapper());
    }

    public List<Article> searchPublic(String keyword, int offset, int limit) {
        String pattern = "%" + keyword + "%";
        return SqlBuilder.select("SELECT a.id, a.knowledge_base_id, a.title, a.summary, a.content, a.content_format, a.author_id, a.status, a.cover_image, a.create_time, a.update_time "
                        + "FROM article a JOIN knowledge_base kb ON a.knowledge_base_id = kb.id "
                        + "WHERE kb.visibility = 1 AND a.status = 1 AND (a.title LIKE ? OR a.content LIKE ?) "
                        + "ORDER BY a.create_time DESC LIMIT ? OFFSET ?")
                .param(pattern).param(pattern).param(limit).param(offset)
                .queryList(mapper());
    }
}
