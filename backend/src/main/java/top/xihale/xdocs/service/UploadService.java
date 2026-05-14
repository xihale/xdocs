package top.xihale.xdocs.service;

import top.xihale.xdocs.dao.UploadFileDao;
import top.xihale.xdocs.po.UploadFile;

import java.util.List;

/**
 * 文件上传服务
 */
public class UploadService {

    private UploadService() {
    }

    /**
     * 记录上传文件
     */
    public static UploadFile uploadFile(String bizType, Integer bizId, String fileName, String fileUrl, Long fileSize, int uploaderId) {
        UploadFile uploadFile = new UploadFile(bizType, bizId, fileName, fileUrl, fileSize, uploaderId);
        UploadFileDao.insert(uploadFile);
        return uploadFile;
    }

    /**
     * 按业务类型和业务ID查询上传文件
     */
    public static List<UploadFile> findByBizTypeAndBizId(String bizType, Integer bizId) {
        return UploadFileDao.findByBizTypeAndBizId(bizType, bizId);
    }
}
