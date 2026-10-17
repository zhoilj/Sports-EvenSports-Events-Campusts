package com.sports.modules.schedule.controller;

import com.sports.common.result.Result;
import com.sports.modules.schedule.dto.CheckinDTO;
import com.sports.modules.schedule.service.CheckinRecordService;
import com.sports.modules.schedule.vo.ScheduleEntryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/checkins")
@RequiredArgsConstructor
public class CheckinRecordController {
    private final CheckinRecordService checkinRecordService;

    @GetMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<ScheduleEntryVO>> listBySchedule(@PathVariable Long scheduleId) {
        return Result.success(checkinRecordService.listBySchedule(scheduleId));
    }

    @PutMapping("/{scheduleEntryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> updateStatus(@PathVariable Long scheduleEntryId, @Valid @RequestBody CheckinDTO dto) {
        checkinRecordService.updateStatus(scheduleEntryId, dto.getStatus(), dto.getRemark());
        return Result.success();
    }
}
