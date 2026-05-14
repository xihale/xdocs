package top.xihale.xdocs.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * TEAM 视图对象，附带成员信息
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamVO {
    private Integer id;
    private String name;
    private String description;
    private Integer ownerId;
    private String ownerName;
    private String ownerAvatar;
    private String avatarUrl;
    private int memberCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
