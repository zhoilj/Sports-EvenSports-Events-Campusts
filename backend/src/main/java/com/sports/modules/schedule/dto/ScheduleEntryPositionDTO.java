package com.sports.modules.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ScheduleEntryPositionDTO {
    @NotNull
    private Long entryId;

    @NotBlank
    @Size(max = 20)
    private String groupNo;

    @Min(1)
    @Max(16)
    private Integer laneNo;

    @NotNull
    @Min(1)
    @Max(1000)
    private Integer startOrder;
}
