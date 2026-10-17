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
        StringBuilder csv = new StringBuilder("\uFEFF排名类别,排名,学院/年级专业,总积分,破纪录项次,第一名,第二名,第三名,有效成绩数\r\n");
        for (Map<String, Object> row : standings) {
            csv.append(csv(row.get("category"))).append(',')
                    .append(row.get("rank")).append(',')
                    .append(csv(row.get("college"))).append(',')
                    .append(row.get("points")).append(',')
                    .append(row.get("recordCount")).append(',')
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
        StringBuilder csv = new StringBuilder("\uFEFF赛事,项目,轮次,比赛日期,参赛者/队伍,学号,学院,成绩,破纪录,状态,名次,积分,发布时间\r\n");
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
                    .append(csv(Objects.equals(result.getIsRecord(), 1) ? "是" : "否")).append(',')
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
        StringBuilder csv = new StringBuilder("\uFEFF赛事,比赛日期,开始时间,结束时间,项目,轮次,组别,道次/顺序,参赛者/队伍,学号,学院,班级/队员名单,检录状态,场地,裁判\r\n");
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
                .orderByAsc(CompetitionResult::getSportItemId)
                .orderByAsc(CompetitionResult::getRanking));
        Map<Long, Schedule> scheduleCache = new HashMap<>();
        Map<Long, SportItem> itemCache = new HashMap<>();
        Map<Long, SysUser> userCache = new HashMap<>();
        Map<Long, RelayTeam> teamCache = new HashMap<>();
        Map<String, Map<String, Object>> aggregates = new HashMap<>();

        for (CompetitionResult result : results) {
            if (result.getScheduleId() != null) {
                Schedule schedule = scheduleCache.computeIfAbsent(result.getScheduleId(), scheduleService::getById);
                if (schedule == null || !("FINAL".equalsIgnoreCase(schedule.getRoundType())
                        || "FIELD_FINAL".equalsIgnoreCase(schedule.getRoundType()))) continue;
            }
            SportItem item = itemCache.computeIfAbsent(result.getSportItemId(), sportItemService::getById);
            if (item == null) continue;
            RelayTeam team = null;
            SysUser user = null;
            if (result.getRelayTeamId() != null) {
                team = teamCache.computeIfAbsent(result.getRelayTeamId(), relayTeamService::getById);
            } else if (result.getUserId() != null) {
                user = userCache.computeIfAbsent(result.getUserId(), sysUserService::getById);
            }
            String college = team != null ? team.getCollege() : (user == null ? null : user.getCollege());
            String className = team != null ? team.getClassName() : (user == null ? null : user.getClassName());
            String division = team != null && Objects.equals(item.getIsTeamEvent(), 1)
                    ? resolveTeamDivision(item, team) : resolveDivision(item);
            String gender = resolveGenderCategory(item);
            if ("A".equals(division)) {
                if ("MALE".equals(gender)) {
                    addStanding(aggregates, "甲组男子", college, result);
                    addStanding(aggregates, "甲组男女团体", college, result);
                } else if ("FEMALE".equals(gender)) {
                    addStanding(aggregates, "甲组女子", college, result);
                    addStanding(aggregates, "甲组男女团体", college, result);
                } else {
                    addStanding(aggregates, "甲组男女团体", college, result);
                }
            } else if ("B".equals(division)) {
                addStanding(aggregates, "乙组年级专业", className, result);
            } else if ("STAFF".equals(division)) {
                addStanding(aggregates, "教工组", college, result);
            } else {
                addStanding(aggregates, "其他组", college, result);
            }
        }

        List<String> categoryOrder = List.of("甲组男子", "甲组女子", "甲组男女团体", "乙组年级专业", "教工组", "其他组");
        List<Map<String, Object>> output = new ArrayList<>();
        for (String category : categoryOrder) {
            List<Map<String, Object>> rows = aggregates.values().stream()
                    .filter(row -> category.equals(row.get("category")))
                    .sorted(standingComparator())
                    .collect(Collectors.toList());
            int sequential = 0;
            int rank = 0;
            Map<String, Object> previous = null;
            for (Map<String, Object> row : rows) {
                sequential++;
                if (previous == null || !sameStanding(previous, row)) rank = sequential;
                row.put("rank", rank);
                output.add(row);
                previous = row;
            }
        }
        return output;
    }

    private void addStanding(Map<String, Map<String, Object>> aggregates, String category,
                             String teamName, CompetitionResult result) {
        String label = teamName == null || teamName.isBlank() ? "未填写学院/年级专业" : teamName.trim();
        String key = category + "::" + label;
        Map<String, Object> row = aggregates.computeIfAbsent(key, ignored -> {
            Map<String, Object> created = new HashMap<>();
            created.put("category", category);
            created.put("college", label);
            created.put("points", 0);
            created.put("recordCount", 0);
            created.put("gold", 0);
            created.put("silver", 0);
            created.put("bronze", 0);
            created.put("resultCount", 0);
            for (int place = 1; place <= 8; place++) created.put("place" + place, 0);
            return created;
        });
        row.put("points", number(row, "points") + (result.getPoints() == null ? 0 : result.getPoints()));
        row.put("resultCount", number(row, "resultCount") + 1);
        if (Objects.equals(result.getIsRecord(), 1)) {
            row.put("recordCount", number(row, "recordCount") + 1);
        }
        if ("VALID".equals(result.getResultStatus()) && result.getRanking() != null
                && result.getRanking() >= 1 && result.getRanking() <= 8) {
            String placeKey = "place" + result.getRanking();
            row.put(placeKey, number(row, placeKey) + 1);
            if (result.getRanking() == 1) row.put("gold", number(row, "gold") + 1);
            if (result.getRanking() == 2) row.put("silver", number(row, "silver") + 1);
            if (result.getRanking() == 3) row.put("bronze", number(row, "bronze") + 1);
        }
    }

    private int number(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private Comparator<Map<String, Object>> standingComparator() {
        Comparator<Map<String, Object>> comparator = Comparator
                .<Map<String, Object>>comparingInt(row -> number(row, "points")).reversed()
                .thenComparing(Comparator.<Map<String, Object>>comparingInt(row -> number(row, "recordCount")).reversed());
        for (int place = 1; place <= 8; place++) {
            String key = "place" + place;
            comparator = comparator.thenComparing(Comparator.<Map<String, Object>>comparingInt(row -> number(row, key)).reversed());
        }
        return comparator.thenComparing(row -> String.valueOf(row.get("college")));
    }

    private boolean sameStanding(Map<String, Object> left, Map<String, Object> right) {
        if (number(left, "points") != number(right, "points")
                || number(left, "recordCount") != number(right, "recordCount")) return false;
        for (int place = 1; place <= 8; place++) {
            String key = "place" + place;
            if (number(left, key) != number(right, key)) return false;
        }
        return true;
    }

    private String resolveDivision(SportItem item) {
        String division = item.getDivision();
        if (division != null && !division.isBlank() && !"OPEN".equalsIgnoreCase(division)) return division.toUpperCase();
        String name = item.getName() == null ? "" : item.getName();
        if (name.contains("甲组")) return "A";
        if (name.contains("乙组")) return "B";
        if (name.contains("教工组")) return "STAFF";
        if (name.contains("集体") || name.contains("民体") || name.contains("趣味")) return "A";
        return "OPEN";
    }

    private String resolveTeamDivision(SportItem item, RelayTeam team) {
        String name = item.getName() == null ? "" : item.getName();
        if (name.contains("教工组") || "STAFF".equalsIgnoreCase(item.getDivision())) return "STAFF";
        String className = team.getClassName() == null ? "" : team.getClassName();
        if (className.matches(".*(社体|体教|体育教育|社会体育|运动训练|休闲体育|体育保健).*")) return "B";
        if ("B".equalsIgnoreCase(item.getDivision()) || name.contains("乙组")) return "B";
        if ("A".equalsIgnoreCase(item.getDivision()) || name.contains("甲组")) return "A";
        return "A";
    }

    private String resolveGenderCategory(SportItem item) {
        String gender = item.getGenderCategory();
        if (gender != null && !gender.isBlank() && !"OPEN".equalsIgnoreCase(gender)) return gender.toUpperCase();
        String name = item.getName() == null ? "" : item.getName();
        if (name.contains("混合接力") || name.contains("混合")) return "MIXED";
        if (name.contains("男子")) return "MALE";
        if (name.contains("女子")) return "FEMALE";
        return "OPEN";
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
