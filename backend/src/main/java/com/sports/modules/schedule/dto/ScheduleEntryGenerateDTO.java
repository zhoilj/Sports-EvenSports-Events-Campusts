package com.sports.modules.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ScheduleEntryGenerateDTO {
    @Min(1) @Max(16)
    private Integer groupSize = 8;
    @Min(1) @Max(16)
    private Integer laneCount = 8;
}
