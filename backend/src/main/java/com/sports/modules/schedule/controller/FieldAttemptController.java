package com.sports.modules.schedule.controller;

import com.sports.common.result.Result;
import com.sports.modules.schedule.dto.FieldAttemptDTO;
import com.sports.modules.schedule.entity.FieldAttempt;
import com.sports.modules.schedule.service.FieldAttemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/field-attempts")
@RequiredArgsConstructor
public class FieldAttemptController {
    private final FieldAttemptService fieldAttemptService;

    @GetMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<FieldAttempt>> listBySchedule(@PathVariable Long scheduleId) {
        return Result.success(fieldAttemptService.listBySchedule(scheduleId));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> saveAttempts(@Valid @RequestBody List<FieldAttemptDTO> attempts) {
        fieldAttemptService.saveAttempts(attempts);
        return Result.success();
    }

    @PostMapping("/schedule/{scheduleId}/finalize")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Integer> finalizeResults(@PathVariable Long scheduleId) {
        return Result.success("田赛成绩已汇总为待复核成绩", fieldAttemptService.finalizeResults(scheduleId));
    }
}
