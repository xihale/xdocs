package top.xihale.xdocs.dao;

import top.xihale.xdocs.po.Comment;
import top.xihale.xdocs.util.BaseMapper;
import top.xihale.xdocs.util.Db;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 评论数据访问层
 */
public class CommentDao {

    private static final BaseMapper<Comment> MAPPER = new BaseMapper<>(Comment.class);

    public static void insert(Comment comment) {
        if (comment.getCreateTime() == null) {
            comment.setCreateTime(LocalDateTime.now());
        }
        MAPPER.insert(comment);
    }

    public static int update(Comment comment) { return MAPPER.update(comment); }
    public static int deleteById(Object id) { return MAPPER.deleteById(id); }
    public static Optional<Comment> findById(Object id) { return MAPPER.findById(id); }
    public static List<Comment> findAll() { return MAPPER.findAll(); }

    public static List<Comment> findByArticleId(int articleId) {
        return MAPPER.findList("article_id = ? ORDER BY create_time ASC", articleId);
    }

    public static void deleteByArticleId(int articleId) {
        Db.sql("DELETE FROM comment WHERE article_id = :articleId")
                .param("articleId", articleId)
                .execute();
    }
}
