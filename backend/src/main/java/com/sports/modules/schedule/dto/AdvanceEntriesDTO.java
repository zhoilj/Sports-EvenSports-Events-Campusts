package com.sports.modules.schedule.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdvanceEntriesDTO {
    @NotNull
    private Long sourceScheduleId;
    @NotNull
    private Long targetScheduleId;
    @Min(1)
    private Integer qualifyCount;
    @Min(0)
    private Integer topPerGroup = 0;
    @Min(1)
    private Integer groupSize = 8;
}
