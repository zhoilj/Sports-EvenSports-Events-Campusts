package com.sports.modules.registration.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 报名 DTO
 *
 * @author sports
 */
@Data
public class RegistrationDTO {

    /**
     * 赛事ID
     */
    @NotNull(message = "赛事ID不能为空")
    private Long eventId;

    /**
     * 运动项目ID
     */
    @NotNull(message = "运动项目ID不能为空")
    private Long sportItemId;

    /**
     * 接力队伍名称；个人报名不需要填写
     */
    @Size(max = 100, message = "接力队伍名称最多100个字符")
    private String teamName;

    /**
     * 接力队员用户ID，按棒次顺序提交；个人报名不需要填写
     */
    @Size(max = 8, message = "接力队员人数不能超过8人")
    private List<Long> memberUserIds;

    /**
     * 报名备注
     */
    @Size(max = 255, message = "报名备注最多255个字符")
    private String remark;
}