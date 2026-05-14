package top.xihale.xdocs.dao;

import top.xihale.xdocs.po.UploadFile;
import top.xihale.xdocs.util.BaseMapper;

import java.util.List;

/**
 * 上传文件数据访问层
 */
public class UploadFileDao {

    private static final BaseMapper<UploadFile> MAPPER = new BaseMapper<>(UploadFile.class);

    public static void insert(UploadFile file) { MAPPER.insert(file); }

    public static List<UploadFile> findByBizTypeAndBizId(String bizType, Integer bizId) {
        return MAPPER.findList("biz_type = ? AND biz_id = ?", bizType, bizId);
    }
}
