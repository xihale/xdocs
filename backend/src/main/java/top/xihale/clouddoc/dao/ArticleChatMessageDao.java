package top.xihale.clouddoc.dao;

import top.xihale.clouddoc.po.ArticleChatMessage;
import top.xihale.clouddoc.util.BaseMapper;
import top.xihale.clouddoc.util.SqlBuilder;

import java.util.List;

/**
 * 文档聊天消息数据访问层
 */
public class ArticleChatMessageDao extends BaseMapper<ArticleChatMessage> {

    public static final ArticleChatMessageDao INSTANCE = new ArticleChatMessageDao();

    public List<ArticleChatMessage> findByArticleId(Integer articleId, int limit) {
        return SqlBuilder.select("SELECT " + columns() + " FROM article_chat_message WHERE article_id = ? ORDER BY create_time DESC LIMIT ?")
                .param(articleId).param(limit)
                .queryList(mapper());
    }
}
