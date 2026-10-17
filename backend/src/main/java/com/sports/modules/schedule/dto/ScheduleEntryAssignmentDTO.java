package com.sports.modules.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 调整单个参赛者的组别、跑道和出场顺序。
 */
@Data
public class ScheduleEntryAssignmentDTO {
    @Size(max = 20, message = "组别名称不能超过20个字符")
    private String groupNo;

    @Min(value = 1, message = "跑道号必须大于0")
    @Max(value = 12, message = "跑道号不能超过12")
    private Integer laneNo;

    @Min(value = 1, message = "出场顺序必须大于0")
    private Integer startOrder;
}
