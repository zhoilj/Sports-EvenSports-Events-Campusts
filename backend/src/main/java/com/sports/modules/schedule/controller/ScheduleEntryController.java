package com.sports.modules.schedule.controller;

import com.sports.common.result.Result;
import com.sports.modules.schedule.dto.CheckinUpdateDTO;
import com.sports.modules.schedule.dto.ScheduleEntryAssignmentDTO;
import com.sports.modules.schedule.service.ScheduleEntryService;
import com.sports.modules.schedule.vo.ScheduleEntryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "竞赛编排与现场检录", description = "自动分组分道、参赛名单维护及现场检录")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScheduleEntryController {

    private final ScheduleEntryService scheduleEntryService;

    @Operation(summary = "查询指定场次的编排与检录名单")
    @GetMapping("/schedules/{scheduleId}/entries")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<ScheduleEntryVO>> listBySchedule(@PathVariable Long scheduleId) {
        return Result.success(scheduleEntryService.listByScheduleId(scheduleId));
    }

    @Operation(summary = "为场次自动生成分组、跑道和检录编号")
    @PostMapping("/schedules/{scheduleId}/entries/auto-assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Integer> autoAssign(@PathVariable Long scheduleId,
                                      @RequestParam(defaultValue = "8") Integer laneCount) {
        int count = scheduleEntryService.autoAssign(scheduleId, laneCount);
        return Result.success("已完成竞赛编排", count);
    }

    @Operation(summary = "调整参赛者的组别、跑道和出场顺序")
    @PutMapping("/schedule-entries/{id}/assignment")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> updateAssignment(@PathVariable Long id,
                                         @Valid @RequestBody ScheduleEntryAssignmentDTO dto) {
        scheduleEntryService.updateAssignment(id, dto);
        return Result.success();
    }

    @Operation(summary = "更新现场检录状态")
    @PostMapping("/schedule-entries/{id}/check-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> checkIn(@PathVariable Long id,
                                @Valid @RequestBody CheckinUpdateDTO dto) {
        scheduleEntryService.checkIn(id, dto);
        return Result.success();
    }

    @Operation(summary = "移除尚未开始场次的参赛编排记录")
    @DeleteMapping("/schedule-entries/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> removeEntry(@PathVariable Long id) {
        scheduleEntryService.removeEntry(id);
        return Result.success();
    }

    @Operation(summary = "查询我的赛程、分组和检录编号")
    @GetMapping("/schedule-entries/my")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<ScheduleEntryVO>> listMyEntries() {
        return Result.success(scheduleEntryService.listMyEntries());
    }
}
