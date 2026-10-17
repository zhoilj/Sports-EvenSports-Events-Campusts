package com.sports.modules.result.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewResultDTO {
    @NotNull @Min(2) @Max(3)
    private Integer status;
    private String remark;
}
