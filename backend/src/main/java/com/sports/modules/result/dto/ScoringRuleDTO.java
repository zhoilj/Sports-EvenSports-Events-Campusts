package com.sports.modules.result.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScoringRuleDTO {
    @NotNull @Min(1)
    private Integer placeNo;
    @NotNull @Min(0)
    private Integer points;
    private String ruleName;
}
