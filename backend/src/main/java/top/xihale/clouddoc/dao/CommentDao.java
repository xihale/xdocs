package top.xihale.clouddoc.dao;

import top.xihale.clouddoc.po.Comment;
import top.xihale.clouddoc.util.BaseMapper;
import top.xihale.clouddoc.util.SqlBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论数据访问层
 */
public class CommentDao extends BaseMapper<Comment> {

    public static final CommentDao INSTANCE = new CommentDao();

    public void insert(Comment comment) {
        if (comment.getCreateTime() == null) {
            comment.setCreateTime(LocalDateTime.now());
        }
        super.insert(comment);
    }

    public List<Comment> findByArticleId(int articleId) {
        return findList("article_id = ? ORDER BY create_time ASC", articleId);
    }

    public void deleteByArticleId(int articleId) {
        SqlBuilder.update("DELETE FROM comment WHERE article_id = ?")
                .param(articleId)
                .execute();
    }
}
