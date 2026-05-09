package top.xihale.xdocs.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.xihale.xdocs.annotation.Id;
import top.xihale.xdocs.annotation.Table;

import java.time.LocalDateTime;

/**
 * TEAM 实体，对应 team 表
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("team")
public class Team {

    @Id
    private Integer id;
    private String name;
    private String description;
    private Integer ownerId;
    private String avatarUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Team(String name, String description, Integer ownerId) {
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
}
