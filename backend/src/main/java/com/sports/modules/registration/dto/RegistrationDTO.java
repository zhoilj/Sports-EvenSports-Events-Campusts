package com.sports.modules.registration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

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
     * 报名备注
     */
    private String remark;
}