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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        String fileName = saveFile(filePart, "image");
        String fileUrl = "/uploads/image/" + fileName;
        UploadService.uploadFile("image", null, fileName, fileUrl, filePart.getSize(), userId);

        Map<String, Object> data = new HashMap<>();
        data.put("url", fileUrl);
        data.put("fileName", fileName);
        return Result.success(data);
    }

    @Post("/avatar")
    private Result<?> handleUploadAvatar(HttpServletRequest req, HttpServletResponse resp) throws IOException, jakarta.servlet.ServletException {
        int userId = getRequiredUserId(req);
        Part filePart = req.getPart("file");
        if (filePart == null) {
            throw new ParamException(ParamError.FILE_REQUIRED);
        }

        String fileName = saveFile(filePart, "avatar");
        String fileUrl = "/uploads/avatar/" + fileName;
        UploadService.uploadFile("avatar", userId, fileName, fileUrl, filePart.getSize(), userId);

        // 同时更新用户头像
        top.xihale.xdocs.service.UserService.updateAvatar(userId, fileUrl);

        Map<String, Object> data = new HashMap<>();
        data.put("url", fileUrl);
        return Result.success(data);
    }

    /**
     * 保存上传文件到磁盘，返回生成的 UUID 文件名（含扩展名）
     */
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

        // 读取上传内容到字节数组（后续需要做 magic number 校验和 SVG sanitize）
        byte[] content = filePart.getInputStream().readAllBytes();

        // Tika magic number 校验
        String detectedType = top.xihale.xdocs.util.FileTypeUtil.detectMimeType(content);
        if (!top.xihale.xdocs.util.FileTypeUtil.isAllowed(detectedType, ext)) {
            throw new ParamException(ParamError.FILE_TYPE_NOT_ALLOWED);
        }

        // SVG sanitize：剥离 script 和事件属性
        if ("svg".equalsIgnoreCase(ext)) {
            content = top.xihale.xdocs.util.SvgSanitizer.sanitize(content);
        }

        // 生成 UUID 文件名
        String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        // 保存到磁盘
        Path dir = Paths.get(StorageConfig.getStoragePath(), bizType);
        Files.createDirectories(dir);
        Path targetPath = dir.resolve(newFileName);
        Files.write(targetPath, content);

        return newFileName;
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }
}
