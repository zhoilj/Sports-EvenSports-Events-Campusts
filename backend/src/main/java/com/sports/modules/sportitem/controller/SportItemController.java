package com.sports.modules.sportitem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.Result;
import com.sports.common.result.ResultCode;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.service.SportItemService;
import com.sports.modules.event.service.EventService;
import com.sports.modules.registration.entity.Registration;
import com.sports.modules.registration.service.RegistrationService;
import com.sports.modules.schedule.entity.Schedule;
import com.sports.modules.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

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
    private final EventService eventService;
    private final RegistrationService registrationService;
    private final ScheduleService scheduleService;

    @Operation(summary = "分页查询运动项目")
    @GetMapping
    public Result<IPage<SportItem>> pageItems(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String competitionGroup) {
        return Result.success(sportItemService.pageItems(current, size, eventId, type, status, competitionGroup));
    }

    @Operation(summary = "按学校秩序册导入甲乙组田径项目")
    @PostMapping("/seed-track-field/{eventId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> seedTrackFieldItems(@PathVariable Long eventId) {
        if (eventService.getById(eventId) == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }

        int created = 0;
        int classified = 0;
        int reused = 0;
        LinkedHashSet<String> skipped = new LinkedHashSet<>();
        for (TrackFieldDefinition definition : trackFieldDefinitions()) {
            boolean blockedByLegacyData = false;
            for (String group : List.of("A", "B")) {
                SportItem existing = sportItemService.getOne(new LambdaQueryWrapper<SportItem>()
                        .eq(SportItem::getEventId, eventId)
                        .eq(SportItem::getName, definition.name)
                        .eq(SportItem::getCompetitionGroup, group)
                        .last("LIMIT 1"));
                if (existing != null) {
                    reused++;
                    continue;
                }

                SportItem unclassified = sportItemService.getOne(new LambdaQueryWrapper<SportItem>()
                        .eq(SportItem::getEventId, eventId)
                        .eq(SportItem::getName, definition.name)
                        .isNull(SportItem::getCompetitionGroup)
                        .last("LIMIT 1"));
                if (unclassified != null) {
                    long registrations = registrationService.count(new LambdaQueryWrapper<Registration>()
                            .eq(Registration::getSportItemId, unclassified.getId()));
                    long schedules = scheduleService.count(new LambdaQueryWrapper<Schedule>()
                            .eq(Schedule::getSportItemId, unclassified.getId()));
                    if (registrations > 0 || schedules > 0) {
                        skipped.add(definition.name + "（已有报名或赛程，需先人工确认归组）");
                        blockedByLegacyData = true;
                        break;
                    }
                    unclassified.setCompetitionGroup(group);
                    sportItemService.updateById(unclassified);
                    classified++;
                    continue;
                }

                SportItem item = new SportItem();
                item.setEventId(eventId);
                item.setName(definition.name);
                item.setType(definition.type);
                item.setCompetitionGroup(group);
                item.setIsRelay(definition.relay ? 1 : 0);
                item.setRelayMemberCount(definition.relay ? 4 : 0);
                item.setRelayReserveCount(0);
                item.setGenderLimit(definition.genderLimit);
                item.setMaxParticipants(0);
                item.setCurrentParticipants(0);
                item.setScoreType(definition.scoreType);
                item.setSortRule(definition.sortRule);
                item.setScoreUnit(definition.scoreUnit);
                item.setRules(definition.rules);
                item.setStatus(1);
                sportItemService.save(item);
                created++;
            }
            if (blockedByLegacyData) {
                // Do not create the other group while a legacy project of the same name is ambiguous.
                continue;
            }
        }

        return Result.success(Map.of(
                "created", created,
                "classified", classified,
                "reused", reused,
                "expected", trackFieldDefinitions().size() * 2,
                "skippedNames", List.copyOf(skipped)
        ));
    }

    private static List<TrackFieldDefinition> trackFieldDefinitions() {
        String generalRule = "参赛单位男、女每项限报3人；个人限报项目数按学校正式竞赛规程执行。";
        String mixedRelayRule = "混合接力第1、3棒为女生，第2、4棒为男生。";
        List<TrackFieldDefinition> items = new ArrayList<>();
        addTime(items, "男子100米", 1, false, generalRule);
        addTime(items, "男子200米", 1, false, generalRule);
        addTime(items, "男子400米", 1, false, generalRule);
        addTime(items, "男子800米", 1, false, generalRule);
        addTime(items, "男子1500米", 1, false, generalRule);
        addTime(items, "男子5000米", 1, false, generalRule);
        addTime(items, "男子110米栏", 1, false, "栏高按学校正式竞赛规程执行。" + generalRule);
        addTime(items, "男子4×100米接力", 1, true, generalRule);
        addTime(items, "男子4×400米接力", 1, true, generalRule);
        addField(items, "男子跳高", 1, generalRule);
        addField(items, "男子跳远", 1, generalRule);
        addField(items, "男子三级跳远", 1, generalRule);
        addField(items, "男子铅球", 1, generalRule);

        addTime(items, "女子100米", 2, false, generalRule);
        addTime(items, "女子200米", 2, false, generalRule);
        addTime(items, "女子400米", 2, false, generalRule);
        addTime(items, "女子800米", 2, false, generalRule);
        addTime(items, "女子1500米", 2, false, generalRule);
        addTime(items, "女子3000米", 2, false, generalRule);
        addTime(items, "女子100米栏", 2, false, "栏高按学校正式竞赛规程执行。" + generalRule);
        addTime(items, "女子4×100米接力", 2, true, generalRule);
        addTime(items, "女子4×400米接力", 2, true, generalRule);
        addField(items, "女子跳高", 2, generalRule);
        addField(items, "女子跳远", 2, generalRule);
        addField(items, "女子三级跳远", 2, generalRule);
        addField(items, "女子铅球", 2, generalRule);

        addTime(items, "4×100米混合接力", 0, true, mixedRelayRule);
        addTime(items, "4×200米混合接力", 0, true, mixedRelayRule);
        return items;
    }

    private static void addTime(List<TrackFieldDefinition> items, String name, int genderLimit,
                                boolean relay, String rules) {
        items.add(new TrackFieldDefinition(name, "TRACK", genderLimit, relay, "TIME", "ASC", "秒", rules));
    }

    private static void addField(List<TrackFieldDefinition> items, String name, int genderLimit, String rules) {
        items.add(new TrackFieldDefinition(name, "FIELD", genderLimit, false, "DISTANCE", "DESC", "米", rules));
    }

    private static final class TrackFieldDefinition {
        private final String name;
        private final String type;
        private final int genderLimit;
        private final boolean relay;
        private final String scoreType;
        private final String sortRule;
        private final String scoreUnit;
        private final String rules;

        private TrackFieldDefinition(String name, String type, int genderLimit, boolean relay,
                                     String scoreType, String sortRule, String scoreUnit, String rules) {
            this.name = name;
            this.type = type;
            this.genderLimit = genderLimit;
            this.relay = relay;
            this.scoreType = scoreType;
            this.sortRule = sortRule;
            this.scoreUnit = scoreUnit;
            this.rules = rules;
        }
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
        validateCompetitionGroup(sportItem);
        // 设置默认值
        if (sportItem.getStatus() == null) {
            sportItem.setStatus(1);
        }
        if (sportItem.getCurrentParticipants() == null) {
            sportItem.setCurrentParticipants(0);
        }
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
        validateCompetitionGroup(sportItem);
        sportItemService.updateById(sportItem);
        return Result.success();
    }

    private void validateCompetitionGroup(SportItem item) {
        if ("TRACK".equalsIgnoreCase(item.getType()) || "FIELD".equalsIgnoreCase(item.getType())) {
            if (!"A".equals(item.getCompetitionGroup()) && !"B".equals(item.getCompetitionGroup())) {
                throw new BusinessException("径赛和田赛项目必须明确配置甲组（普通本科组）或乙组（体育专业组）");
            }
        }
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
