package com.sports.modules.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FieldAttemptDTO {
    private Long id;
    @NotNull
    private Long scheduleEntryId;
    @NotNull @Min(1) @Max(6)
    private Integer attemptNo;
    private BigDecimal score;
    private Boolean foul = false;
    private String remark;
}
