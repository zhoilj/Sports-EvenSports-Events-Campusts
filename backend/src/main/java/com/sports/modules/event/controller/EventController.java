package com.sports.modules.event.controller;

import com.sports.common.result.PageResult;
import com.sports.common.result.Result;
import com.sports.modules.event.dto.EventDTO;
import com.sports.modules.event.dto.EventQueryDTO;
import com.sports.modules.event.service.EventService;
import com.sports.modules.event.vo.EventVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 赛事 Controller
 *
 * @author sports
 */
@Tag(name = "赛事管理", description = "赛事的增删改查")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @Operation(summary = "分页查询赛事")
    @GetMapping
    public Result<PageResult<EventVO>> pageEvents(EventQueryDTO queryDTO) {
        return Result.success(PageResult.of(eventService.pageEvents(queryDTO)));
    }

    @Operation(summary = "获取赛事详情")
    @GetMapping("/{id}")
    public Result<EventVO> getEventDetail(@PathVariable Long id) {
        return Result.success(eventService.getEventDetail(id));
    }

    @Operation(summary = "创建赛事")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> createEvent(@Valid @RequestBody EventDTO eventDTO) {
        Long id = eventService.createEvent(eventDTO);
        return Result.success("创建成功", id);
    }

    @Operation(summary = "更新赛事")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateEvent(@PathVariable Long id, @Valid @RequestBody EventDTO eventDTO) {
        eventService.updateEvent(id, eventDTO);
        return Result.success();
    }

    @Operation(summary = "删除赛事")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return Result.success();
    }

    @Operation(summary = "发布赛事")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> publishEvent(@PathVariable Long id) {
        eventService.publishEvent(id);
        return Result.success("发布成功", null);
    }

    @Operation(summary = "取消赛事")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> cancelEvent(@PathVariable Long id) {
        eventService.cancelEvent(id);
        return Result.success("取消成功", null);
    }

    @Operation(summary = "获取可报名的赛事列表")
    @GetMapping("/available")
    public Result<List<EventVO>> getAvailableEvents() {
        return Result.success(eventService.getAvailableEvents());
    }

    @Operation(summary = "获取进行中的赛事列表")
    @GetMapping("/ongoing")
    public Result<List<EventVO>> getOngoingEvents() {
        return Result.success(eventService.getOngoingEvents());
    }
}