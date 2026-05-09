package top.xihale.xdocs.exception;

import lombok.Getter;

/**
 * 文章相关异常
 */
@Getter
public class ArticleException extends BusinessException {
    private final ArticleError error;

    public ArticleException(ArticleError error) {
        super(error);
        this.error = error;
    }

    /**
     * 文章模块所有可能的业务错误
     */
    @Getter
    public enum ArticleError implements ErrorCode {
        KB_NOT_FOUND(404, "知识库不存在"),
        ARTICLE_NOT_FOUND(404, "文章不存在"),
        NOT_ARTICLE_OWNER(403, "只有文章作者才能操作"),
        NO_EDIT_PERMISSION(403, "无权编辑此文章"),
        COMMENT_NOT_FOUND(404, "评论不存在"),
        CANNOT_DELETE_OTHERS_COMMENT(403, "只能删除自己的评论"),
        ;

        private final int code;
        private final String message;

        ArticleError(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
