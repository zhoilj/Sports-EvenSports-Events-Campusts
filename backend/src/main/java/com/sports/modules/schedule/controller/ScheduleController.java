package com.sports.modules.schedule.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.Result;
import com.sports.common.result.ResultCode;
import com.sports.modules.schedule.entity.Schedule;
import com.sports.modules.schedule.service.ScheduleService;
import com.sports.modules.schedule.service.ScheduleEntryService;
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
        // 设置默认状态
        if (schedule.getStatus() == null) {
            schedule.setStatus(1);
        }
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
        if (scheduleEntryService.hasEntries(id)
                && (!java.util.Objects.equals(existing.getEventId(), schedule.getEventId())
                || !java.util.Objects.equals(existing.getSportItemId(), schedule.getSportItemId()))) {
            throw new BusinessException("该场次已生成参赛编排，不能更换所属赛事或运动项目");
        }
        schedule.setId(id);
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
        if (scheduleEntryService.hasEntries(id)) {
            throw new BusinessException("该场次已有参赛编排记录，请先移除参赛记录后再删除赛程");
        }
        scheduleService.removeById(id);
        return Result.success();
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
