package com.sports.modules.sportitem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.Result;
import com.sports.common.result.ResultCode;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.service.SportItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 运动项目 Controller
 *
 * @author sports
 */
@Tag(name = "运动项目管理", description = "运动项目的增删改查")
@RestController
@RequestMapping("/api/sport-items")
@RequiredArgsConstructor
public class SportItemController {

    private final SportItemService sportItemService;

    @Operation(summary = "分页查询运动项目")
    @GetMapping
    public Result<IPage<SportItem>> pageItems(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status) {
        return Result.success(sportItemService.pageItems(current, size, eventId, type, status));
    }

    @Operation(summary = "根据赛事ID获取项目列表")
    @GetMapping("/event/{eventId}")
    public Result<List<SportItem>> getByEventId(@PathVariable Long eventId) {
        return Result.success(sportItemService.getByEventId(eventId));
    }

    @Operation(summary = "获取项目详情")
    @GetMapping("/{id}")
    public Result<SportItem> getById(@PathVariable Long id) {
        SportItem item = sportItemService.getById(id);
        if (item == null) {
            throw new BusinessException(ResultCode.SPORT_ITEM_NOT_FOUND);
        }
        return Result.success(item);
    }

    @Operation(summary = "创建运动项目")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> create(@Valid @RequestBody SportItem sportItem) {
        // 设置默认值
        if (sportItem.getStatus() == null) {
            sportItem.setStatus(1);
        }
        if (sportItem.getCurrentParticipants() == null) {
            sportItem.setCurrentParticipants(0);
        }
        normalizeParticipationConfig(sportItem, null);
        sportItemService.save(sportItem);
        return Result.success(sportItem.getId());
    }

    @Operation(summary = "更新运动项目")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SportItem sportItem) {
        SportItem existing = sportItemService.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.SPORT_ITEM_NOT_FOUND);
        }
        sportItem.setId(id);
        normalizeParticipationConfig(sportItem, existing);
        sportItemService.updateById(sportItem);
        return Result.success();
    }

    @Operation(summary = "删除运动项目")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        SportItem existing = sportItemService.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.SPORT_ITEM_NOT_FOUND);
        }
        sportItemService.removeById(id);
        return Result.success();
    }

    private void normalizeParticipationConfig(SportItem item, SportItem existing) {
        String mode = item.getParticipationMode();
        if (!org.springframework.util.StringUtils.hasText(mode) && existing != null) {
            mode = existing.getParticipationMode();
        }
        if (!org.springframework.util.StringUtils.hasText(mode)) {
            mode = "INDIVIDUAL";
        }
        mode = mode.trim().toUpperCase(java.util.Locale.ROOT);
        if (!"INDIVIDUAL".equals(mode) && !"RELAY".equals(mode)) {
            throw new BusinessException("参赛模式只能为 INDIVIDUAL 或 RELAY");
        }
        item.setParticipationMode(mode);
        if ("RELAY".equals(mode)) {
            Integer memberCount = item.getTeamMemberCount() != null
                    ? item.getTeamMemberCount()
                    : existing == null ? null : existing.getTeamMemberCount();
            if (memberCount == null || memberCount < 2 || memberCount > 8) {
                throw new BusinessException("接力项目必须配置2至8名队员");
            }
            item.setTeamMemberCount(memberCount);
        } else {
            item.setTeamMemberCount(null);
        }
    }

    @Operation(summary = "切换项目状态")
    @PostMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        SportItem item = sportItemService.getById(id);
        if (item == null) {
            throw new BusinessException(ResultCode.SPORT_ITEM_NOT_FOUND);
        }
        item.setStatus(item.getStatus() == 1 ? 0 : 1);
        sportItemService.updateById(item);
        return Result.success();
    }
}
