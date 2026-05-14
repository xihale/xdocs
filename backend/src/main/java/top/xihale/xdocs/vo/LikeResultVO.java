package top.xihale.xdocs.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 点赞结果视图对象
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LikeResultVO {
    private boolean liked;
    private int likeCount;
}
