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
    private final com.sports.modules.event.service.EventService eventService;

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
        if (sportItem.getStatus() == null) sportItem.setStatus(1);
        if (sportItem.getCurrentParticipants() == null) sportItem.setCurrentParticipants(0);
        if (sportItem.getDivision() == null) sportItem.setDivision("OPEN");
        if (sportItem.getGenderCategory() == null) sportItem.setGenderCategory("OPEN");
        if (sportItem.getRoundPolicy() == null) sportItem.setRoundPolicy("FLEXIBLE");
        if (sportItem.getScoringMultiplier() == null) sportItem.setScoringMultiplier(
                Integer.valueOf(1).equals(sportItem.getIsRelay()) ? 2 : 1);
        if (sportItem.getParticipationPoints() == null) sportItem.setParticipationPoints(0);
        if (sportItem.getRecordPoints() == null) sportItem.setRecordPoints(9);
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
        sportItemService.updateById(sportItem);
        return Result.success();
    }

    @Operation(summary = "按正式秩序册匹配项目默认规则")
    @PostMapping("/event/{eventId}/apply-orderbook-defaults")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Integer> applyOrderBookDefaults(@PathVariable Long eventId) {
        if (eventService.getById(eventId) == null) throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        List<SportItem> items = sportItemService.getByEventId(eventId);
        if (items == null || items.isEmpty()) throw new BusinessException("该赛事尚未创建比赛项目");
        int updated = 0;
        for (SportItem item : items) {
            String name = item.getName() == null ? "" : item.getName().replaceAll("\\s", "");
            boolean groupEvent = containsAny(name, "集体", "民体", "趣味", "拔河", "跳长绳", "两人三足",
                    "三人板鞋", "抛绣球", "背篓接绣球", "校园越野跑", "车轮滚滚", "旱地龙舟",
                    "袋鼠跳", "滚铁环", "企鹅漫步", "协同大转盘", "竞步跑");
            boolean mixedRelay = name.contains("混合接力") || name.contains("混合");
            item.setDivision(name.contains("甲组") ? "A" : name.contains("乙组") ? "B"
                    : name.contains("教工组") ? "STAFF" : groupEvent ? "A" : "OPEN");
            item.setGenderCategory(mixedRelay ? "MIXED" : name.contains("男子") ? "MALE"
                    : name.contains("女子") ? "FEMALE" : Integer.valueOf(1).equals(item.getGenderLimit()) ? "MALE"
                    : Integer.valueOf(2).equals(item.getGenderLimit()) ? "FEMALE" : "OPEN");
            boolean prelimFinal = !mixedRelay && "TRACK".equalsIgnoreCase(item.getType())
                    && (name.contains("100米") || name.contains("200米")
                    || name.matches(".*4[×xX]100米接力.*"));
            item.setRoundPolicy(prelimFinal ? "PRELIMINARY_FINAL" : "FINAL_ONLY");
            item.setScoringMultiplier(Integer.valueOf(1).equals(item.getIsRelay()) || groupEvent ? 2 : 1);
            item.setParticipationPoints(groupEvent ? 3 : 0);
            item.setRecordPoints(9);
            sportItemService.updateById(item);
            updated++;
        }
        return Result.success("已匹配默认规则，请逐项对照秩序册复核", updated);
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) return true;
        }
        return false;
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
