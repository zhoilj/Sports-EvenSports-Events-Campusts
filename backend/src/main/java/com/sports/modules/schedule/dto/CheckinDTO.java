package com.sports.modules.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckinDTO {
    @NotBlank
    private String status;
    private String remark;
}
