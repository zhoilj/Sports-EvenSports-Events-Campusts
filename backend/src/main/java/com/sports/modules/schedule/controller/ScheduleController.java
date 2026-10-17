package com.sports.modules.schedule.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.Result;
import com.sports.common.result.ResultCode;
import com.sports.modules.schedule.entity.Schedule;
import com.sports.modules.schedule.dto.ScheduleEntryGenerateDTO;
import com.sports.modules.schedule.dto.AdvanceEntriesDTO;
import com.sports.modules.schedule.service.ScheduleEntryService;
import com.sports.modules.schedule.vo.ScheduleEntryVO;
import com.sports.modules.schedule.vo.ScheduleConflictVO;
import com.sports.modules.schedule.service.ScheduleService;
import com.sports.modules.schedule.vo.ScheduleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 赛程 Controller
 *
 * @author sports
 */
@Tag(name = "赛程管理", description = "赛程的增删改查")
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ScheduleEntryService scheduleEntryService;

    @Operation(summary = "分页查询赛程")
    @GetMapping
    public Result<IPage<ScheduleVO>> pageSchedules(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) String matchDate) {
        return Result.success(scheduleService.pageSchedulesVO(current, size, eventId, matchDate));
    }

    @Operation(summary = "获取赛程详情")
    @GetMapping("/{id}")
    public Result<Schedule> getById(@PathVariable Long id) {
        Schedule schedule = scheduleService.getById(id);
        if (schedule == null) {
            throw new BusinessException(ResultCode.SCHEDULE_NOT_FOUND);
        }
        return Result.success(schedule);
    }

    @Operation(summary = "创建赛程")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> create(@Valid @RequestBody Schedule schedule) {
        // 设置默认状态和轮次类型。
        if (schedule.getStatus() == null) schedule.setStatus(1);
        if (schedule.getRoundType() == null) schedule.setRoundType("FINAL");
        scheduleService.validateSchedule(schedule, null);
        scheduleService.save(schedule);
        return Result.success(schedule.getId());
    }

    @Operation(summary = "更新赛程")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Schedule schedule) {
        Schedule existing = scheduleService.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.SCHEDULE_NOT_FOUND);
        }
        schedule.setId(id);
        if (schedule.getRoundType() == null) schedule.setRoundType(existing.getRoundType() == null ? "FINAL" : existing.getRoundType());
        scheduleService.validateSchedule(schedule, id);
        scheduleService.updateById(schedule);
        return Result.success();
    }

    @Operation(summary = "删除赛程")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        Schedule existing = scheduleService.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.SCHEDULE_NOT_FOUND);
        }
        scheduleService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "查询赛程参赛名单")
    @GetMapping("/{id}/entries")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<java.util.List<ScheduleEntryVO>> getEntries(@PathVariable Long id) {
        return Result.success(scheduleEntryService.listEntryDetails(id));
    }

    @Operation(summary = "检查参赛者赛程时间冲突")
    @GetMapping("/{id}/conflicts")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<java.util.List<ScheduleConflictVO>> getConflicts(@PathVariable Long id) {
        return Result.success(scheduleEntryService.checkTimeConflicts(id));
    }

    @Operation(summary = "按已通过报名生成分组分道")
    @PostMapping("/{id}/entries/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Integer> generateEntries(@PathVariable Long id,
                                           @Valid @RequestBody(required = false) ScheduleEntryGenerateDTO dto) {
        int groupSize = dto == null || dto.getGroupSize() == null ? 8 : dto.getGroupSize();
        int laneCount = dto == null || dto.getLaneCount() == null ? 8 : dto.getLaneCount();
        return Result.success("已生成参赛名单", scheduleEntryService.generateEntries(id, groupSize, laneCount));
    }

    @Operation(summary = "按预赛成绩晋级下一轮")
    @PostMapping("/advance")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Integer> advanceEntries(@Valid @RequestBody AdvanceEntriesDTO dto) {
        return Result.success("晋级名单已生成", scheduleEntryService.advanceEntries(dto));
    }

    @Operation(summary = "开始比赛")
    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> startMatch(@PathVariable Long id) {
        scheduleService.startMatch(id);
        return Result.success();
    }

    @Operation(summary = "完成比赛")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> completeMatch(@PathVariable Long id) {
        scheduleService.completeMatch(id);
        return Result.success();
    }
}
