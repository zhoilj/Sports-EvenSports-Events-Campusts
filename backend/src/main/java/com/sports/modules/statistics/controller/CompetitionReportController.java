package com.sports.modules.statistics.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.Result;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.service.EventService;
import com.sports.modules.registration.entity.RelayTeam;
import com.sports.modules.registration.service.RelayTeamService;
import com.sports.modules.result.entity.CompetitionResult;
import com.sports.modules.result.service.CompetitionResultService;
import com.sports.modules.schedule.entity.Schedule;
import com.sports.modules.schedule.service.ScheduleEntryService;
import com.sports.modules.schedule.service.ScheduleService;
import com.sports.modules.schedule.vo.ScheduleEntryVO;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.service.SportItemService;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class CompetitionReportController {
    private final EventService eventService;
    private final ScheduleService scheduleService;
    private final ScheduleEntryService scheduleEntryService;
    private final CompetitionResultService competitionResultService;
    private final SportItemService sportItemService;
    private final SysUserService sysUserService;
    private final RelayTeamService relayTeamService;

    @GetMapping("/standings")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<Map<String, Object>>> getStandings(@RequestParam Long eventId) {
        return Result.success(buildStandings(eventId));
    }

    @GetMapping("/standings.csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<String> exportStandings(@RequestParam Long eventId) {
        List<Map<String, Object>> standings = buildStandings(eventId);
        StringBuilder csv = new StringBuilder("\uFEFF学院,总积分,金牌数,银牌数,铜牌数,有效成绩数\r\n");
        for (Map<String, Object> row : standings) {
            csv.append(csv(row.get("college"))).append(',')
                    .append(row.get("points")).append(',')
                    .append(row.get("gold")).append(',')
                    .append(row.get("silver")).append(',')
                    .append(row.get("bronze")).append(',')
                    .append(row.get("resultCount")).append("\r\n");
        }
        return Result.success(csv.toString());
    }

    @GetMapping("/results.csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<String> exportResults(@RequestParam Long eventId) {
        Event event = eventService.getById(eventId);
        if (event == null) throw new BusinessException("赛事不存在");
        List<CompetitionResult> results = competitionResultService.list(new LambdaQueryWrapper<CompetitionResult>()
                .eq(CompetitionResult::getEventId, eventId)
                .eq(CompetitionResult::getIsPublished, 1)
                .orderByAsc(CompetitionResult::getSportItemId)
                .orderByAsc(CompetitionResult::getRanking));
        StringBuilder csv = new StringBuilder("\uFEFF赛事,项目,轮次,比赛日期,参赛者/队伍,学号,学院,成绩,状态,名次,积分,发布时间\r\n");
        for (CompetitionResult result : results) {
            SportItem item = sportItemService.getById(result.getSportItemId());
            Schedule schedule = result.getScheduleId() == null ? null : scheduleService.getById(result.getScheduleId());
            String participant = "";
            String studentNo = "";
            String college = "";
            if (result.getRelayTeamId() != null) {
                RelayTeam team = relayTeamService.getById(result.getRelayTeamId());
                if (team != null) {
                    participant = team.getTeamName();
                    college = team.getCollege();
                }
            } else if (result.getUserId() != null) {
                SysUser user = sysUserService.getById(result.getUserId());
                if (user != null) {
                    participant = user.getRealName() == null ? user.getUsername() : user.getRealName();
                    studentNo = user.getStudentNo();
                    college = user.getCollege();
                }
            }
            String score = result.getScoreText();
            if ((score == null || score.isBlank()) && result.getScore() != null) {
                score = result.getScore().stripTrailingZeros().toPlainString() + (item == null || item.getScoreUnit() == null ? "" : item.getScoreUnit());
            }
            csv.append(csv(event.getName())).append(',')
                    .append(csv(item == null ? "" : item.getName())).append(',')
                    .append(csv(schedule == null ? "历史成绩" : roundName(schedule.getRoundType()))).append(',')
                    .append(csv(schedule == null || schedule.getMatchDate() == null ? "" : schedule.getMatchDate().toString())).append(',')
                    .append(csv(participant)).append(',')
                    .append(csv(studentNo)).append(',')
                    .append(csv(college)).append(',')
                    .append(csv(score)).append(',')
                    .append(csv(result.getResultStatus())).append(',')
                    .append(csv(result.getRanking())).append(',')
                    .append(csv(result.getPoints())).append(',')
                    .append(csv(result.getPublishedAt())).append("\r\n");
        }
        return Result.success(csv.toString());
    }

    @GetMapping("/order-book.csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<String> exportOrderBook(@RequestParam Long eventId) {
        Event event = eventService.getById(eventId);
        if (event == null) throw new BusinessException("赛事不存在");
        List<Schedule> schedules = scheduleService.list(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getEventId, eventId)
                .orderByAsc(Schedule::getMatchDate)
                .orderByAsc(Schedule::getStartTime)
                .orderByAsc(Schedule::getSportItemId)
                .orderByAsc(Schedule::getRoundNo));
        StringBuilder csv = new StringBuilder("\uFEFF赛事,比赛日期,开始时间,结束时间,项目,轮次,组别,道次/顺序,检录编号,参赛者/队伍,学号,学院,班级/队员名单,检录状态,场地,裁判\r\n");
        for (Schedule schedule : schedules) {
            SportItem item = sportItemService.getById(schedule.getSportItemId());
            List<ScheduleEntryVO> entries = scheduleEntryService.listEntryDetails(schedule.getId());
            if (entries.isEmpty()) {
                appendOrderBookRow(csv, event, schedule, item, null);
            } else {
                for (ScheduleEntryVO entry : entries) appendOrderBookRow(csv, event, schedule, item, entry);
            }
        }
        return Result.success(csv.toString());
    }

    private void appendOrderBookRow(StringBuilder csv, Event event, Schedule schedule, SportItem item, ScheduleEntryVO entry) {
        String order = entry == null ? "" : (entry.getLaneNo() == null
                ? String.valueOf(entry.getStartOrder() == null ? "" : entry.getStartOrder())
                : String.valueOf(entry.getLaneNo()));
        csv.append(csv(event.getName())).append(',')
                .append(csv(schedule.getMatchDate())).append(',')
                .append(csv(schedule.getStartTime())).append(',')
                .append(csv(schedule.getEndTime())).append(',')
                .append(csv(item == null ? "" : item.getName())).append(',')
                .append(csv(roundName(schedule.getRoundType()))).append(',')
                .append(csv(entry == null ? schedule.getGroupNo() : entry.getGroupNo())).append(',')
                .append(csv(order)).append(',')
                .append(csv(entry == null ? "" : entry.getCheckinCode())).append(',')
                .append(csv(entry == null ? "" : entry.getParticipantName())).append(',')
                .append(csv(entry == null ? "" : entry.getStudentNo())).append(',')
                .append(csv(entry == null ? "" : entry.getCollege())).append(',')
                .append(csv(entry == null ? "" : (entry.getMemberNames() == null ? entry.getClassName() : entry.getMemberNames()))).append(',')
                .append(csv(entry == null ? "" : entry.getCheckinStatus())).append(',')
                .append(csv(schedule.getLocation())).append(',')
                .append(csv(schedule.getReferee())).append("\r\n");
    }

    private List<Map<String, Object>> buildStandings(Long eventId) {
        if (eventService.getById(eventId) == null) throw new BusinessException("赛事不存在");
        List<CompetitionResult> results = competitionResultService.list(new LambdaQueryWrapper<CompetitionResult>()
                .eq(CompetitionResult::getEventId, eventId)
                .eq(CompetitionResult::getIsPublished, 1)
                .eq(CompetitionResult::getResultStatus, "VALID")
                .isNotNull(CompetitionResult::getRanking));
        Map<Long, Schedule> scheduleCache = new HashMap<>();
        Map<String, Map<String, Object>> byCollege = new TreeMap<>();
        for (CompetitionResult result : results) {
            if (result.getScheduleId() != null) {
                Schedule schedule = scheduleCache.computeIfAbsent(result.getScheduleId(), scheduleService::getById);
                if (schedule != null && !("FINAL".equalsIgnoreCase(schedule.getRoundType())
                        || "FIELD_FINAL".equalsIgnoreCase(schedule.getRoundType()))) continue;
            }
            String college = "";
            if (result.getRelayTeamId() != null) {
                RelayTeam team = relayTeamService.getById(result.getRelayTeamId());
                if (team != null) college = team.getCollege();
            } else if (result.getUserId() != null) {
                SysUser user = sysUserService.getById(result.getUserId());
                if (user != null) college = user.getCollege();
            }
            if (college == null || college.isBlank()) college = "未填写学院";
            Map<String, Object> row = byCollege.computeIfAbsent(college, key -> {
                Map<String, Object> item = new HashMap<>();
                item.put("college", key);
                item.put("points", 0);
                item.put("gold", 0);
                item.put("silver", 0);
                item.put("bronze", 0);
                item.put("resultCount", 0);
                return item;
            });
            row.put("points", ((Number) row.get("points")).intValue() + (result.getPoints() == null ? 0 : result.getPoints()));
            row.put("resultCount", ((Number) row.get("resultCount")).intValue() + 1);
            if (Objects.equals(result.getRanking(), 1)) row.put("gold", ((Number) row.get("gold")).intValue() + 1);
            if (Objects.equals(result.getRanking(), 2)) row.put("silver", ((Number) row.get("silver")).intValue() + 1);
            if (Objects.equals(result.getRanking(), 3)) row.put("bronze", ((Number) row.get("bronze")).intValue() + 1);
        }
        return byCollege.values().stream().sorted(
                Comparator.<Map<String, Object>>comparingInt(row -> ((Number) row.get("points")).intValue()).reversed()
                        .thenComparing(Comparator.<Map<String, Object>>comparingInt(row -> ((Number) row.get("gold")).intValue()).reversed())
                        .thenComparing(Comparator.<Map<String, Object>>comparingInt(row -> ((Number) row.get("silver")).intValue()).reversed())
                        .thenComparing(Comparator.<Map<String, Object>>comparingInt(row -> ((Number) row.get("bronze")).intValue()).reversed())
                        .thenComparing(row -> String.valueOf(row.get("college"))))
                .collect(Collectors.toList());
    }

    private String roundName(String type) {
        if (type == null) return "";
        return switch (type) {
            case "PRELIMINARY" -> "预赛";
            case "SEMIFINAL" -> "半决赛";
            case "FINAL", "FIELD_FINAL" -> "决赛";
            default -> type;
        };
    }

    private String csv(Object value) {
        if (value == null) return "";
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\r") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
