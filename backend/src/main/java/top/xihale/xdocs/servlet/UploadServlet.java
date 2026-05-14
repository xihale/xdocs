package top.xihale.xdocs.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import top.xihale.xdocs.config.StorageConfig;
import top.xihale.xdocs.exception.ParamException;
import top.xihale.xdocs.exception.ParamException.ParamError;
import top.xihale.xdocs.service.UploadService;
import top.xihale.xdocs.servlet.route.Post;
import top.xihale.xdocs.util.Result;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传接口
 */
@WebServlet("/api/upload/*")
@jakarta.servlet.annotation.MultipartConfig
public class UploadServlet extends BaseServlet {

    @Post("/image")
    private Result<?> handleUploadImage(HttpServletRequest req, HttpServletResponse resp) throws IOException, jakarta.servlet.ServletException {
        int userId = getRequiredUserId(req);
        Part filePart = req.getPart("file");
        if (filePart == null) {
            throw new ParamException(ParamError.FILE_REQUIRED);
        }

        String fileUrl = saveFile(filePart, "image");
        UploadService.uploadFile("image", null, filePart.getSubmittedFileName(), fileUrl, filePart.getSize(), userId);

        Map<String, Object> data = new HashMap<>();
        data.put("url", fileUrl);
        data.put("fileName", filePart.getSubmittedFileName());
        return Result.success(data);
    }

    @Post("/avatar")
    private Result<?> handleUploadAvatar(HttpServletRequest req, HttpServletResponse resp) throws IOException, jakarta.servlet.ServletException {
        int userId = getRequiredUserId(req);
        Part filePart = req.getPart("file");
        if (filePart == null) {
            throw new ParamException(ParamError.FILE_REQUIRED);
        }

        String fileUrl = saveFile(filePart, "avatar");
        UploadService.uploadFile("avatar", userId, filePart.getSubmittedFileName(), fileUrl, filePart.getSize(), userId);

        // 同时更新用户头像
        top.xihale.xdocs.service.UserService.updateAvatar(userId, fileUrl);

        Map<String, Object> data = new HashMap<>();
        data.put("url", fileUrl);
        return Result.success(data);
    }

    private String saveFile(Part filePart, String bizType) throws IOException {
        String originalName = filePart.getSubmittedFileName();
        if (originalName == null || originalName.isBlank()) {
            throw new ParamException(ParamError.FILE_NAME_EMPTY);
        }

        // 校验扩展名
        String ext = getExtension(originalName);
        if (!StorageConfig.getAllowedExtensions().contains(ext.toLowerCase())) {
            throw new ParamException(ParamError.FILE_TYPE_NOT_ALLOWED);
        }

        // 校验大小
        if (filePart.getSize() > StorageConfig.getMaxSizeBytes()) {
            throw new ParamException(ParamError.FILE_TOO_LARGE);
        }

        // 生成 UUID 文件名
        String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        // 保存到磁盘
        Path dir = Paths.get(StorageConfig.getStoragePath(), bizType);
        Files.createDirectories(dir);
        Path targetPath = dir.resolve(newFileName);

        try (InputStream in = filePart.getInputStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // 返回 URL 路径
        return "/uploads/" + bizType + "/" + newFileName;
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }
}
