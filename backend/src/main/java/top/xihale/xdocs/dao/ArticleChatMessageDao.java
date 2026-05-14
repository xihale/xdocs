package top.xihale.xdocs.dao;

import top.xihale.xdocs.po.ArticleChatMessage;
import top.xihale.xdocs.util.BaseMapper;
import top.xihale.xdocs.util.Db;

import java.util.List;

/**
 * 文档聊天消息数据访问层
 */
public class ArticleChatMessageDao {

    private static final BaseMapper<ArticleChatMessage> MAPPER = new BaseMapper<>(ArticleChatMessage.class);

    private static final String SQL_FIND_BY_ARTICLE_ID =
            "SELECT %s FROM article_chat_message WHERE article_id = :articleId ORDER BY create_time DESC LIMIT :limit".formatted(MAPPER.columns());

    public static void insert(ArticleChatMessage message) { MAPPER.insert(message); }

    public static List<ArticleChatMessage> findByArticleId(Integer articleId, int limit) {
        return Db.sql(SQL_FIND_BY_ARTICLE_ID)
                .param("articleId", articleId).param("limit", limit)
                .query(MAPPER.mapper())
                .list();
    }
}
