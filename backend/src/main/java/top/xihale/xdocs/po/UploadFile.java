package top.xihale.xdocs.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.xihale.xdocs.annotation.Id;
import top.xihale.xdocs.annotation.Table;

import java.time.LocalDateTime;

/**
 * 上传文件实体，对应 upload_file 表
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("upload_file")
public class UploadFile {

    @Id
    private Integer id;
    private String bizType;
    private Integer bizId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private Integer uploaderId;
    private LocalDateTime createTime;

    public UploadFile(String bizType, Integer bizId, String fileName, String fileUrl, Long fileSize, Integer uploaderId) {
        this.bizType = bizType;
        this.bizId = bizId;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileSize = fileSize;
        this.uploaderId = uploaderId;
        this.createTime = LocalDateTime.now();
    }
}
