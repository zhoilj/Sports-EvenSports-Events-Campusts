package com.sports.modules.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.common.exception.BusinessException;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.service.EventService;
import com.sports.modules.registration.entity.Registration;
import com.sports.modules.registration.entity.RelayTeam;
import com.sports.modules.registration.entity.RelayTeamMember;
import com.sports.modules.registration.mapper.RelayTeamMemberMapper;
import com.sports.modules.registration.service.RegistrationService;
import com.sports.modules.registration.service.RelayTeamService;
import com.sports.modules.result.entity.CompetitionResult;
import com.sports.modules.result.service.CompetitionResultService;
import com.sports.modules.schedule.dto.AdvanceEntriesDTO;
import com.sports.modules.schedule.entity.CheckinRecord;
import com.sports.modules.schedule.entity.Schedule;
import com.sports.modules.schedule.entity.ScheduleEntry;
import com.sports.modules.schedule.mapper.CheckinRecordMapper;
import com.sports.modules.schedule.mapper.ScheduleEntryMapper;
import com.sports.modules.schedule.service.ScheduleEntryService;
import com.sports.modules.schedule.service.ScheduleService;
import com.sports.modules.schedule.vo.ScheduleConflictVO;
import com.sports.modules.schedule.vo.ScheduleEntryVO;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.service.SportItemService;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleEntryServiceImpl extends ServiceImpl<ScheduleEntryMapper, ScheduleEntry>
        implements ScheduleEntryService {

    private static final int TRANSITION_BUFFER_SECONDS = 600;

    private final ScheduleService scheduleService;
    private final EventService eventService;
    private final SportItemService sportItemService;
    private final RegistrationService registrationService;
    private final RelayTeamService relayTeamService;
    private final RelayTeamMemberMapper relayTeamMemberMapper;
    private final CheckinRecordMapper checkinRecordMapper;
    private final CompetitionResultService competitionResultService;
    private final SysUserService sysUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateEntries(Long scheduleId, Integer groupSize, Integer laneCount) {
        Schedule schedule = requireSchedule(scheduleId);
        if (schedule.getStatus() == null || schedule.getStatus() != 1) {
            throw new BusinessException("只能为尚未开始的赛程生成参赛名单");
        }
        if (this.count(new LambdaQueryWrapper<ScheduleEntry>().eq(ScheduleEntry::getScheduleId, scheduleId)) > 0) {
            throw new BusinessException("该赛程已经生成参赛名单，不能重复生成");
        }
        if (groupSize == null || groupSize < 1 || groupSize > 16
                || laneCount == null || laneCount < 1 || laneCount > 16) {
            throw new BusinessException("每组人数和道数必须在1至16之间");
        }

        SportItem item = sportItemService.getById(schedule.getSportItemId());
        if (item == null) throw new BusinessException("比赛项目不存在");
        boolean relay = Objects.equals(item.getIsRelay(), 1);
        boolean field = "FIELD".equalsIgnoreCase(item.getType());
        if (!field && !relay && groupSize > laneCount) {
            throw new BusinessException("径赛每组人数不能超过可用道数");
        }
        if ("FINAL".equalsIgnoreCase(schedule.getRoundType()) || "SEMIFINAL".equalsIgnoreCase(schedule.getRoundType())) {
            List<Schedule> earlierRounds = scheduleService.list(new LambdaQueryWrapper<Schedule>()
                    .eq(Schedule::getEventId, schedule.getEventId())
                    .eq(Schedule::getSportItemId, schedule.getSportItemId())
                    .lt(Schedule::getRoundNo, schedule.getRoundNo())
                    .in(Schedule::getRoundType, "PRELIMINARY", "SEMIFINAL"));
            boolean hasEarlierEntries = earlierRounds.stream().anyMatch(s ->
                    this.count(new LambdaQueryWrapper<ScheduleEntry>().eq(ScheduleEntry::getScheduleId, s.getId())) > 0);
            if (hasEarlierEntries) {
                throw new BusinessException("该项目已有上一轮参赛名单，请使用“按成绩晋级”生成本轮名单");
            }
        }

        List<ScheduleEntry> candidates = new ArrayList<>();
        if (relay) {
            List<RelayTeam> teams = relayTeamService.list(new LambdaQueryWrapper<RelayTeam>()
                    .eq(RelayTeam::getEventId, schedule.getEventId())
                    .eq(RelayTeam::getSportItemId, schedule.getSportItemId())
                    .eq(RelayTeam::getStatus, 2)
                    .orderByAsc(RelayTeam::getId));
            for (RelayTeam team : teams) {
                ScheduleEntry entry = new ScheduleEntry();
                entry.setScheduleId(scheduleId);
                entry.setRelayTeamId(team.getId());
                entry.setQualificationStatus("ENTERED");
                candidates.add(entry);
            }
        } else {
            List<Registration> registrations = registrationService.list(new LambdaQueryWrapper<Registration>()
                    .eq(Registration::getEventId, schedule.getEventId())
                    .eq(Registration::getSportItemId, schedule.getSportItemId())
                    .eq(Registration::getStatus, 2)
                    .orderByAsc(Registration::getId));
            for (Registration registration : registrations) {
                ScheduleEntry entry = new ScheduleEntry();
                entry.setScheduleId(scheduleId);
                entry.setRegistrationId(registration.getId());
                entry.setQualificationStatus("ENTERED");
                candidates.add(entry);
            }
        }
        if (candidates.isEmpty()) {
            throw new BusinessException(relay ? "该项目没有已通过审核的接力队伍" : "该项目没有已通过审核的报名记录");
        }

        int perGroup = field ? candidates.size() : Math.min(groupSize, laneCount);
        for (int i = 0; i < candidates.size(); i++) {
            ScheduleEntry entry = candidates.get(i);
            entry.setStartOrder(i + 1);
            entry.setGroupNo(field ? "A" : groupLabel(i / perGroup));
            entry.setLaneNo(field ? null : (i % perGroup) + 1);
        }
        List<ScheduleConflictVO> conflicts = findConflicts(schedule, candidates);
        if (!conflicts.isEmpty()) {
            String message = conflicts.stream().limit(5).map(ScheduleConflictVO::getMessage).collect(Collectors.joining("；"));
            throw new BusinessException("发现参赛者赛程时间冲突，未生成名单：" + message);
        }

        for (ScheduleEntry entry : candidates) {
            this.save(entry);
            CheckinRecord checkin = new CheckinRecord();
            checkin.setScheduleEntryId(entry.getId());
            checkin.setStatus("PENDING");
            checkinRecordMapper.insert(checkin);
        }
        return candidates.size();
    }

    @Override
    public List<ScheduleEntryVO> listEntryDetails(Long scheduleId) {
        Schedule schedule = requireSchedule(scheduleId);
        Event event = eventService.getById(schedule.getEventId());
        SportItem scheduleItem = sportItemService.getById(schedule.getSportItemId());
        List<ScheduleEntry> entries = this.list(new LambdaQueryWrapper<ScheduleEntry>()
                .eq(ScheduleEntry::getScheduleId, scheduleId)
                .orderByAsc(ScheduleEntry::getGroupNo)
                .orderByAsc(ScheduleEntry::getLaneNo)
                .orderByAsc(ScheduleEntry::getStartOrder));
        Map<String, CompetitionResult> results = competitionResultService.list(
                        new LambdaQueryWrapper<CompetitionResult>().eq(CompetitionResult::getScheduleId, scheduleId))
                .stream().collect(Collectors.toMap(this::resultKey, r -> r, (a, b) -> a));
        List<ScheduleEntryVO> output = new ArrayList<>();
        for (ScheduleEntry entry : entries) {
            ScheduleEntryVO vo = new ScheduleEntryVO();
            vo.setId(entry.getId());
            vo.setScheduleId(entry.getScheduleId());
            vo.setRegistrationId(entry.getRegistrationId());
            vo.setRelayTeamId(entry.getRelayTeamId());
            vo.setGroupNo(entry.getGroupNo());
            vo.setLaneNo(entry.getLaneNo());
            vo.setStartOrder(entry.getStartOrder());
            vo.setQualificationStatus(entry.getQualificationStatus());
            vo.setCheckinCode("CK" + String.format(java.util.Locale.ROOT, "%08d", entry.getId()));
            vo.setEventName(event == null ? null : event.getName());
            vo.setSportItemName(scheduleItem == null ? null : scheduleItem.getName());
            vo.setScheduleName(schedule.getScheduleName());
            vo.setMatchDate(schedule.getMatchDate());
            vo.setStartTime(schedule.getStartTime());
            vo.setLocation(schedule.getLocation());

            CheckinRecord checkin = checkinRecordMapper.selectOne(new LambdaQueryWrapper<CheckinRecord>()
                    .eq(CheckinRecord::getScheduleEntryId, entry.getId()));
            vo.setCheckinStatus(checkin == null || checkin.getStatus() == null ? "PENDING" : checkin.getStatus());
            if (checkin != null) {
                vo.setCheckedAt(checkin.getCheckedAt());
                vo.setCheckinRemark(checkin.getRemark());
            }

            if (entry.getRegistrationId() != null) {
                Registration registration = registrationService.getById(entry.getRegistrationId());
                if (registration != null) {
                    SysUser user = sysUserService.getById(registration.getUserId());
                    if (user != null) {
                        vo.setParticipantName(user.getRealName() == null ? user.getUsername() : user.getRealName());
                        vo.setStudentNo(user.getStudentNo());
                        vo.setCollege(user.getCollege());
                        vo.setClassName(user.getClassName());
                    }
                }
            } else if (entry.getRelayTeamId() != null) {
                RelayTeam team = relayTeamService.getById(entry.getRelayTeamId());
                if (team != null) {
                    vo.setTeamName(team.getTeamName());
                    vo.setParticipantName(team.getTeamName());
                    vo.setCollege(team.getCollege());
                    List<String> names = relayTeamMemberMapper.selectList(new LambdaQueryWrapper<RelayTeamMember>()
                                    .eq(RelayTeamMember::getTeamId, team.getId())
                                    .orderByAsc(RelayTeamMember::getLegOrder))
                            .stream().map(m -> {
                                SysUser user = sysUserService.getById(m.getUserId());
                                String name = user == null ? String.valueOf(m.getUserId())
                                        : (user.getRealName() == null ? user.getUsername() : user.getRealName());
                                return Boolean.TRUE.equals(m.getReserve() != null && m.getReserve() == 1)
                                        ? name + "（替补）" : name;
                            }).collect(Collectors.toList());
                    vo.setMemberNames(String.join("、", names));
                }
            }
            CompetitionResult result = results.get(entry.getRegistrationId() != null
                    ? "R:" + entry.getRegistrationId() : "T:" + entry.getRelayTeamId());
            if (result != null) {
                vo.setScore(result.getScore());
                vo.setScoreText(result.getScoreText());
                vo.setResultStatus(result.getResultStatus());
                vo.setRanking(result.getRanking());
                vo.setReviewStatus(result.getReviewStatus());
                vo.setPublished(Objects.equals(result.getIsPublished(), 1));
            }
            output.add(vo);
        }
        return output;
    }

    @Override
    public List<ScheduleEntryVO> listMyEntries(Long userId) {
        if (userId == null) return List.of();

        List<Long> registrationIds = registrationService.list(new LambdaQueryWrapper<Registration>()
                        .eq(Registration::getUserId, userId)
                        .eq(Registration::getStatus, 2))
                .stream().map(Registration::getId).collect(Collectors.toList());

        List<Long> memberTeamIds = relayTeamMemberMapper.selectList(new LambdaQueryWrapper<RelayTeamMember>()
                        .eq(RelayTeamMember::getUserId, userId))
                .stream().map(RelayTeamMember::getTeamId).distinct().collect(Collectors.toList());
        List<Long> approvedTeamIds = memberTeamIds.isEmpty()
                ? List.of()
                : relayTeamService.list(new LambdaQueryWrapper<RelayTeam>()
                                .in(RelayTeam::getId, memberTeamIds)
                                .eq(RelayTeam::getStatus, 2))
                        .stream().map(RelayTeam::getId).collect(Collectors.toList());

        if (registrationIds.isEmpty() && approvedTeamIds.isEmpty()) return List.of();

        LambdaQueryWrapper<ScheduleEntry> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(condition -> {
            if (!registrationIds.isEmpty()) {
                condition.in(ScheduleEntry::getRegistrationId, registrationIds);
            }
            if (!approvedTeamIds.isEmpty()) {
                if (!registrationIds.isEmpty()) condition.or();
                condition.in(ScheduleEntry::getRelayTeamId, approvedTeamIds);
            }
        }).orderByAsc(ScheduleEntry::getScheduleId).orderByAsc(ScheduleEntry::getStartOrder);

        List<ScheduleEntry> mine = this.list(wrapper);
        if (mine.isEmpty()) return List.of();

        Map<Long, Set<Long>> idsBySchedule = mine.stream().collect(Collectors.groupingBy(
                ScheduleEntry::getScheduleId,
                Collectors.mapping(ScheduleEntry::getId, Collectors.toSet())));
        List<ScheduleEntryVO> output = new ArrayList<>();
        for (Map.Entry<Long, Set<Long>> group : idsBySchedule.entrySet()) {
            for (ScheduleEntryVO entry : listEntryDetails(group.getKey())) {
                if (group.getValue().contains(entry.getId())) output.add(entry);
            }
        }
        output.sort(Comparator.comparing(ScheduleEntryVO::getMatchDate,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ScheduleEntryVO::getStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return output;
    }

    @Override
    public List<ScheduleConflictVO> checkTimeConflicts(Long scheduleId) {
        Schedule schedule = requireSchedule(scheduleId);
        List<ScheduleEntry> entries = this.list(new LambdaQueryWrapper<ScheduleEntry>()
                .eq(ScheduleEntry::getScheduleId, scheduleId));
        return findConflicts(schedule, entries);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int advanceEntries(AdvanceEntriesDTO dto) {
        if (dto == null || dto.getSourceScheduleId() == null || dto.getTargetScheduleId() == null) {
            throw new BusinessException("请选择晋级来源赛程和目标赛程");
        }
        Schedule source = requireSchedule(dto.getSourceScheduleId());
        Schedule target = requireSchedule(dto.getTargetScheduleId());
        if (Objects.equals(source.getId(), target.getId())) throw new BusinessException("晋级来源和目标赛程不能相同");
        if (!Objects.equals(source.getEventId(), target.getEventId())
                || !Objects.equals(source.getSportItemId(), target.getSportItemId())) {
            throw new BusinessException("来源赛程与目标赛程必须属于同一赛事和项目");
        }
        if (source.getStatus() == null || source.getStatus() != 3) {
            throw new BusinessException("来源赛程必须先结束");
        }
        if (target.getStatus() == null || target.getStatus() != 1) {
            throw new BusinessException("目标赛程必须处于未开始状态");
        }
        if ("FIELD".equalsIgnoreCase(sportItemService.getById(target.getSportItemId()).getType())) {
            throw new BusinessException("田赛项目不使用径赛晋级编排");
        }
        if (this.count(new LambdaQueryWrapper<ScheduleEntry>().eq(ScheduleEntry::getScheduleId, target.getId())) > 0) {
            throw new BusinessException("目标赛程已有参赛名单，不能重复晋级");
        }
        int qualifyCount = dto.getQualifyCount();
        int topPerGroup = dto.getTopPerGroup() == null ? 0 : dto.getTopPerGroup();
        int groupSize = dto.getGroupSize() == null ? 8 : dto.getGroupSize();
        if (qualifyCount < 1 || topPerGroup < 0 || groupSize < 1 || groupSize > 16) {
            throw new BusinessException("晋级人数、每组晋级名额或目标组容量设置无效");
        }

        SportItem item = sportItemService.getById(source.getSportItemId());
        boolean ascending = "ASC".equalsIgnoreCase(item.getSortRule())
                || (item.getSortRule() == null && "TIME".equalsIgnoreCase(item.getScoreType()));
        List<ScheduleEntry> sourceEntries = this.list(new LambdaQueryWrapper<ScheduleEntry>()
                .eq(ScheduleEntry::getScheduleId, source.getId()));
        Map<String, ScheduleEntry> entryMap = sourceEntries.stream()
                .collect(Collectors.toMap(this::entryKey, e -> e, (a, b) -> a));
        List<CompetitionResult> validResults = competitionResultService.list(new LambdaQueryWrapper<CompetitionResult>()
                .eq(CompetitionResult::getScheduleId, source.getId())
                .eq(CompetitionResult::getReviewStatus, 2)
                .eq(CompetitionResult::getResultStatus, "VALID")
                .isNotNull(CompetitionResult::getScore));
        List<CompetitionResult> eligible = validResults.stream()
                .filter(r -> entryMap.containsKey(resultKey(r))).collect(Collectors.toList());
        Comparator<CompetitionResult> scoreOrder = Comparator.comparing(CompetitionResult::getScore);
        if (!ascending) scoreOrder = scoreOrder.reversed();
        eligible.sort(scoreOrder);
        if (eligible.isEmpty()) throw new BusinessException("来源赛程没有复核通过的有效成绩");

        Map<String, List<CompetitionResult>> byGroup = new LinkedHashMap<>();
        for (CompetitionResult result : eligible) {
            ScheduleEntry entry = entryMap.get(resultKey(result));
            String group = entry.getGroupNo() == null ? "A" : entry.getGroupNo();
            byGroup.computeIfAbsent(group, key -> new ArrayList<>()).add(result);
        }
        Set<String> selectedKeys = new HashSet<>();
        List<CompetitionResult> selected = new ArrayList<>();
        if (topPerGroup > 0) {
            for (List<CompetitionResult> groupResults : byGroup.values()) {
                groupResults.sort(scoreOrder);
                for (int i = 0; i < Math.min(topPerGroup, groupResults.size()); i++) {
                    CompetitionResult result = groupResults.get(i);
                    if (selectedKeys.add(resultKey(result))) selected.add(result);
                }
            }
        }
        if (selected.size() > qualifyCount) {
            throw new BusinessException("各组保送晋级人数已超过总晋级人数，请调整规则");
        }
        for (CompetitionResult result : eligible) {
            if (selected.size() >= qualifyCount) break;
            if (selectedKeys.add(resultKey(result))) selected.add(result);
        }
        if (selected.isEmpty()) throw new BusinessException("没有符合条件的晋级选手");

        List<ScheduleEntry> candidates = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            CompetitionResult result = selected.get(i);
            ScheduleEntry sourceEntry = entryMap.get(resultKey(result));
            ScheduleEntry entry = new ScheduleEntry();
            entry.setScheduleId(target.getId());
            entry.setRegistrationId(sourceEntry.getRegistrationId());
            entry.setRelayTeamId(sourceEntry.getRelayTeamId());
            entry.setSeedScore(result.getScore());
            entry.setQualificationStatus("QUALIFIED");
            entry.setStartOrder(i + 1);
            entry.setGroupNo(groupLabel(i / groupSize));
            entry.setLaneNo((i % groupSize) + 1);
            candidates.add(entry);
        }
        List<ScheduleConflictVO> conflicts = findConflicts(target, candidates);
        if (!conflicts.isEmpty()) {
            String message = conflicts.stream().limit(5).map(ScheduleConflictVO::getMessage).collect(Collectors.joining("；"));
            throw new BusinessException("晋级名单存在赛程时间冲突：" + message);
        }
        for (ScheduleEntry entry : candidates) {
            this.save(entry);
            CheckinRecord checkin = new CheckinRecord();
            checkin.setScheduleEntryId(entry.getId());
            checkin.setStatus("PENDING");
            checkinRecordMapper.insert(checkin);
        }
        return candidates.size();
    }

    private List<ScheduleConflictVO> findConflicts(Schedule schedule, List<ScheduleEntry> candidates) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        Map<Long, ScheduleEntry> ownerByUser = new HashMap<>();
        List<ScheduleConflictVO> conflicts = new ArrayList<>();
        for (ScheduleEntry candidate : candidates) {
            for (Long userId : participantIds(candidate)) {
                ScheduleEntry previous = ownerByUser.putIfAbsent(userId, candidate);
                if (previous != null && !Objects.equals(previous.getId(), candidate.getId())) {
                    conflicts.add(buildConflict(schedule, schedule, userId,
                            "同一参赛者重复出现在当前赛程的多个参赛名额中"));
                }
            }
        }

        int targetStart = seconds(schedule.getStartTime()) - TRANSITION_BUFFER_SECONDS;
        int targetEnd = seconds(effectiveEnd(schedule)) + TRANSITION_BUFFER_SECONDS;
        List<Schedule> sameDay = scheduleService.list(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getMatchDate, schedule.getMatchDate())
                .ne(Schedule::getId, schedule.getId())
                .ne(Schedule::getStatus, 4));
        for (Schedule other : sameDay) {
            int otherStart = seconds(other.getStartTime()) - TRANSITION_BUFFER_SECONDS;
            int otherEnd = seconds(effectiveEnd(other)) + TRANSITION_BUFFER_SECONDS;
            if (targetStart >= otherEnd || otherStart >= targetEnd) continue;
            List<ScheduleEntry> existingEntries = this.list(new LambdaQueryWrapper<ScheduleEntry>()
                    .eq(ScheduleEntry::getScheduleId, other.getId()));
            Map<Long, ScheduleEntry> otherOwnerByUser = new HashMap<>();
            for (ScheduleEntry existing : existingEntries) {
                for (Long userId : participantIds(existing)) otherOwnerByUser.putIfAbsent(userId, existing);
            }
            for (Map.Entry<Long, ScheduleEntry> candidateOwner : ownerByUser.entrySet()) {
                if (otherOwnerByUser.containsKey(candidateOwner.getKey())) {
                    conflicts.add(buildConflict(schedule, other, candidateOwner.getKey(),
                            "参赛者在相近时段还有另一场比赛"));
                }
            }
        }
        return conflicts;
    }

    private ScheduleConflictVO buildConflict(Schedule current, Schedule other, Long userId, String reason) {
        ScheduleConflictVO vo = new ScheduleConflictVO();
        vo.setUserId(userId);
        SysUser user = sysUserService.getById(userId);
        vo.setParticipantName(user == null ? String.valueOf(userId)
                : (user.getRealName() == null ? user.getUsername() : user.getRealName()));
        vo.setCurrentScheduleId(current.getId());
        vo.setCurrentScheduleName(current.getScheduleName());
        vo.setCurrentTime(timeRange(current));
        vo.setConflictingScheduleId(other.getId());
        vo.setConflictingScheduleName(other.getScheduleName());
        vo.setConflictingTime(timeRange(other));
        vo.setLocation(other.getLocation());
        vo.setMessage(vo.getParticipantName() + "：" + reason + "（"
                + current.getScheduleName() + " " + timeRange(current) + "；"
                + other.getScheduleName() + " " + timeRange(other) + "）");
        return vo;
    }

    private Set<Long> participantIds(ScheduleEntry entry) {
        if (entry.getRegistrationId() != null) {
            Registration registration = registrationService.getById(entry.getRegistrationId());
            return registration == null || registration.getUserId() == null
                    ? Set.of() : Set.of(registration.getUserId());
        }
        if (entry.getRelayTeamId() != null) {
            return relayTeamMemberMapper.selectList(new LambdaQueryWrapper<RelayTeamMember>()
                            .eq(RelayTeamMember::getTeamId, entry.getRelayTeamId()))
                    .stream().map(RelayTeamMember::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        }
        return Set.of();
    }

    private Schedule requireSchedule(Long id) {
        Schedule schedule = scheduleService.getById(id);
        if (schedule == null) throw new BusinessException("赛程不存在");
        return schedule;
    }

    private String resultKey(CompetitionResult result) {
        return result.getRelayTeamId() != null ? "T:" + result.getRelayTeamId() : "R:" + result.getRegistrationId();
    }

    private String entryKey(ScheduleEntry entry) {
        return entry.getRelayTeamId() != null ? "T:" + entry.getRelayTeamId() : "R:" + entry.getRegistrationId();
    }

    private String groupLabel(int index) {
        if (index < 26) return String.valueOf((char) ('A' + index));
        return "G" + (index + 1);
    }

    private LocalTime effectiveEnd(Schedule schedule) {
        if (schedule.getEndTime() != null) return schedule.getEndTime();
        return schedule.getStartTime().plusHours(1);
    }

    private int seconds(LocalTime time) {
        return time.toSecondOfDay();
    }

    private String timeRange(Schedule schedule) {
        return schedule.getStartTime() + "—" + effectiveEnd(schedule);
    }
}
