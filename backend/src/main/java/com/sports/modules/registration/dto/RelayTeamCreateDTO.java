package com.sports.modules.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RelayTeamCreateDTO {
    @NotNull(message = "赛事不能为空")
    private Long eventId;

    @NotNull(message = "接力项目不能为空")
    private Long sportItemId;

    @NotBlank(message = "队伍名称不能为空")
    @Size(max = 100, message = "队伍名称不能超过100个字符")
    private String teamName;

    /** Formal runners first, then reserve runners. The creator is added if omitted. */
    @NotEmpty(message = "请至少选择一名队员")
    private List<Long> memberUserIds;
}
