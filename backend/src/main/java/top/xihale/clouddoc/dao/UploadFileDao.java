package top.xihale.clouddoc.dao;

import top.xihale.clouddoc.po.UploadFile;
import top.xihale.clouddoc.util.BaseMapper;

import java.util.List;

/**
 * 上传文件数据访问层
 */
public class UploadFileDao extends BaseMapper<UploadFile> {

    public static final UploadFileDao INSTANCE = new UploadFileDao();

    public List<UploadFile> findByBizTypeAndBizId(String bizType, Integer bizId) {
        return findList("biz_type = ? AND biz_id = ?", bizType, bizId);
    }
}
