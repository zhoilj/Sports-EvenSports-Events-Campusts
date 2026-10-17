package com.sports.modules.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 现场检录状态变更请求。
 */
@Data
public class CheckinUpdateDTO {
    @NotBlank(message = "检录状态不能为空")
    private String status;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}
