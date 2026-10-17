package com.sports.modules.result.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.common.exception.BusinessException;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.service.EventService;
import com.sports.modules.registration.entity.Registration;
import com.sports.modules.registration.entity.RelayTeam;
import com.sports.modules.registration.service.RegistrationService;
import com.sports.modules.registration.service.RelayTeamService;
import com.sports.modules.result.entity.CompetitionResult;
import com.sports.modules.result.entity.ResultAuditLog;
import com.sports.modules.result.entity.ScoringRule;
import com.sports.modules.result.mapper.CompetitionResultMapper;
import com.sports.modules.result.mapper.ResultAuditLogMapper;
import com.sports.modules.result.mapper.ScoringRuleMapper;
import com.sports.modules.result.service.CompetitionResultService;
import com.sports.modules.result.vo.CompetitionResultVO;
import com.sports.modules.schedule.entity.Schedule;
import com.sports.modules.schedule.entity.ScheduleEntry;
import com.sports.modules.schedule.mapper.ScheduleEntryMapper;
import com.sports.modules.schedule.service.ScheduleService;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.service.SportItemService;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.service.SysUserService;
import com.sports.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompetitionResultServiceImpl extends ServiceImpl<CompetitionResultMapper, CompetitionResult>
        implements CompetitionResultService {

    private final EventService eventService;
    private final SportItemService sportItemService;
    private final SysUserService sysUserService;
    private final RegistrationService registrationService;
    private final RelayTeamService relayTeamService;
    private final ScheduleService scheduleService;
    private final ScheduleEntryMapper scheduleEntryMapper;
    private final ScoringRuleMapper scoringRuleMapper;
    private final ResultAuditLogMapper resultAuditLogMapper;

    @Override
    public IPage<CompetitionResult> pageResults(Integer current, Integer size, Long eventId, Long sportItemId, Long userId) {
        Page<CompetitionResult> page = new Page<>(current, size);
        LambdaQueryWrapper<CompetitionResult> wrapper = new LambdaQueryWrapper<CompetitionResult>()
                .eq(eventId != null, CompetitionResult::getEventId, eventId)
                .eq(sportItemId != null, CompetitionResult::getSportItemId, sportItemId)
                .eq(userId != null, CompetitionResult::getUserId, userId)
                .orderByAsc(CompetitionResult::getRanking)
                .orderByDesc(CompetitionResult::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public IPage<CompetitionResultVO> pageResultsVO(Integer current, Integer size, Long eventId, Long sportItemId, Long userId) {
        return convertToVOPage(pageResults(current, size, eventId, sportItemId, userId));
    }

    @Override
    public List<CompetitionResult> getRankingByEventAndItem(Long eventId, Long sportItemId) {
        return this.list(new LambdaQueryWrapper<CompetitionResult>()
                        .eq(CompetitionResult::getEventId, eventId)
                        .eq(CompetitionResult::getSportItemId, sportItemId)
                        .eq(CompetitionResult::getIsPublished, 1)
                        .orderByAsc(CompetitionResult::getRanking))
                .stream().filter(result -> {
                    if (result.getScheduleId() == null) return true;
                    Schedule schedule = scheduleService.getById(result.getScheduleId());
                    return schedule != null && ("FINAL".equalsIgnoreCase(schedule.getRoundType())
                            || "FIELD_FINAL".equalsIgnoreCase(schedule.getRoundType()));
                }).collect(Collectors.toList());
    }

    @Override
    public List<CompetitionResult> getMyResults(Long userId) {
        return this.list(new LambdaQueryWrapper<CompetitionResult>()
                .eq(CompetitionResult::getUserId, userId)
                .eq(CompetitionResult::getIsPublished, 1)
                .orderByDesc(CompetitionResult::getPublishedAt));
    }

    @Override
    public List<CompetitionResultVO> getMyResultsVO(Long userId) {
        return convertToVOList(getMyResults(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSaveResults(List<CompetitionResult> results) {
        if (results == null || results.isEmpty()) return;
        Long eventId = results.get(0).getEventId();
        Long sportItemId = results.get(0).getSportItemId();
        Long scheduleId = results.get(0).getScheduleId();
        List<CompetitionResult> before = new ArrayList<>();
        for (CompetitionResult result : results) {
            if (!Objects.equals(eventId, result.getEventId())
                    || !Objects.equals(sportItemId, result.getSportItemId())
                    || !Objects.equals(scheduleId, result.getScheduleId())) {
                throw new BusinessException("批量录入成绩必须属于同一赛事、项目和场次");
            }
            CompetitionResult existing = result.getId() == null ? null : this.getById(result.getId());
            if (existing != null && Objects.equals(existing.getIsPublished(), 1)) {
                throw new BusinessException("正式发布的成绩不能直接修改，请联系管理员处理");
            }
            before.add(existing);
            prepareAndValidateResult(result);
        }
        for (int i = 0; i < results.size(); i++) {
            CompetitionResult result = results.get(i);
            this.saveOrUpdate(result);
        }
        recalculateRankings(eventId, sportItemId, scheduleId);
        for (int i = 0; i < results.size(); i++) {
            CompetitionResult saved = this.getById(results.get(i).getId());
            writeAudit(saved, before.get(i) == null ? "CREATED" : "UPDATED",
                    before.get(i), saved, "批量录入/更新成绩");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveValidatedResult(CompetitionResult result) {
        if (result == null) throw new BusinessException("成绩数据不能为空");
        if (result.getId() == null && result.getScheduleId() != null) {
            LambdaQueryWrapper<CompetitionResult> existingQuery = new LambdaQueryWrapper<CompetitionResult>()
                    .eq(CompetitionResult::getScheduleId, result.getScheduleId());
            if (result.getRelayTeamId() != null) {
                existingQuery.eq(CompetitionResult::getRelayTeamId, result.getRelayTeamId());
            } else if (result.getRegistrationId() != null) {
                existingQuery.eq(CompetitionResult::getRegistrationId, result.getRegistrationId());
            }
            CompetitionResult existing = this.getOne(existingQuery, false);
            if (existing != null) result.setId(existing.getId());
        }
        CompetitionResult before = result.getId() == null ? null : this.getById(result.getId());
        if (result.getId() != null && before == null) throw new BusinessException("成绩记录不存在");
        if (before != null && Objects.equals(before.getIsPublished(), 1)) {
            throw new BusinessException("正式发布的成绩不能直接修改");
        }
        prepareAndValidateResult(result);
        this.saveOrUpdate(result);
        recalculateRankings(result.getEventId(), result.getSportItemId(), result.getScheduleId());
        CompetitionResult saved = this.getById(result.getId());
        writeAudit(saved, before == null ? "CREATED" : "UPDATED", before, saved, result.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewResult(Long id, Integer status, String remark) {
        if (status == null || (status != 2 && status != 3)) {
            throw new BusinessException("成绩复核状态只能为通过或退回");
        }
        CompetitionResult result = this.getById(id);
        if (result == null) throw new BusinessException("成绩记录不存在");
        if (Objects.equals(result.getIsPublished(), 1)) throw new BusinessException("正式发布后的成绩不能直接复核");
        if (Objects.equals(result.getReviewStatus(), 2) && status == 2) {
            throw new BusinessException("该成绩已复核通过");
        }
        CompetitionResult before = copyResult(result);
        result.setReviewStatus(status);
        result.setReviewerId(SecurityUtils.getUserId());
        result.setReviewTime(LocalDateTime.now());
        result.setReviewRemark(remark);
        this.updateById(result);
        writeAudit(result, status == 2 ? "REVIEWED" : "RETURNED", before, result, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int publishScheduleResults(Long scheduleId) {
        Schedule schedule = scheduleService.getById(scheduleId);
        if (schedule == null) throw new BusinessException("赛程不存在");
        if (schedule.getStatus() == null || schedule.getStatus() != 3) {
            throw new BusinessException("请先将赛程标记为已结束，再发布正式成绩");
        }
        List<ScheduleEntry> entries = scheduleEntryMapper.selectList(new LambdaQueryWrapper<ScheduleEntry>()
                .eq(ScheduleEntry::getScheduleId, scheduleId));
        if (entries.isEmpty()) throw new BusinessException("该赛程尚未生成参赛名单");

        List<CompetitionResult> results = this.list(new LambdaQueryWrapper<CompetitionResult>()
                .eq(CompetitionResult::getScheduleId, scheduleId));
        Map<String, CompetitionResult> resultByParticipant = results.stream()
                .collect(Collectors.toMap(this::participantKey, r -> r, (left, right) -> left));
        for (ScheduleEntry entry : entries) {
            String key = entry.getRegistrationId() != null
                    ? "R:" + entry.getRegistrationId() : "T:" + entry.getRelayTeamId();
            CompetitionResult result = resultByParticipant.get(key);
            if (result == null) {
                throw new BusinessException("参赛名单尚有未录入成绩的选手/队伍，不能发布正式成绩");
            }
            if (!Objects.equals(result.getReviewStatus(), 2)) {
                throw new BusinessException("所有参赛成绩必须先复核通过，才能正式发布");
            }
        }
        if (results.isEmpty()) throw new BusinessException("该赛程暂无成绩记录");

        Map<Integer, Integer> pointsByPlace = scoringRuleMapper.selectList(new LambdaQueryWrapper<ScoringRule>()
                        .eq(ScoringRule::getEventId, schedule.getEventId()))
                .stream().collect(Collectors.toMap(ScoringRule::getPlaceNo, ScoringRule::getPoints, (a, b) -> a));
        LocalDateTime now = LocalDateTime.now();
        Long operator = SecurityUtils.getUserId();
        for (CompetitionResult result : results) {
            CompetitionResult before = copyResult(result);
            result.setPoints("VALID".equals(result.getResultStatus()) && result.getRanking() != null
                    ? pointsByPlace.getOrDefault(result.getRanking(), 0) : 0);
            result.setIsPublished(1);
            result.setPublishedBy(operator);
            result.setPublishedAt(now);
            this.updateById(result);
            writeAudit(result, "PUBLISHED", before, result, "赛程成绩正式发布");
        }
        return results.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResult(Long id) {
        CompetitionResult result = this.getById(id);
        if (result == null) throw new BusinessException("成绩记录不存在");
        if (Objects.equals(result.getIsPublished(), 1)) {
            throw new BusinessException("正式发布的成绩不能删除");
        }
        writeAudit(result, "DELETED", result, null, "删除成绩记录");
        this.removeById(id);
    }

    private void prepareAndValidateResult(CompetitionResult result) {
        if (result.getEventId() == null || result.getSportItemId() == null) {
            throw new BusinessException("赛事和比赛项目不能为空");
        }
        SportItem item = sportItemService.getById(result.getSportItemId());
        if (item == null || !Objects.equals(item.getEventId(), result.getEventId())) {
            throw new BusinessException("成绩所属项目与赛事不一致");
        }
        if (result.getScheduleId() != null) {
            Schedule schedule = scheduleService.getById(result.getScheduleId());
            if (schedule == null || !Objects.equals(schedule.getEventId(), result.getEventId())
                    || !Objects.equals(schedule.getSportItemId(), result.getSportItemId())) {
                throw new BusinessException("成绩所属赛程与赛事/项目不一致");
            }
            long entryCount = scheduleEntryMapper.selectCount(new LambdaQueryWrapper<ScheduleEntry>()
                    .eq(ScheduleEntry::getScheduleId, schedule.getId()));
            if (entryCount > 0) {
                long matchingEntry = scheduleEntryMapper.selectCount(new LambdaQueryWrapper<ScheduleEntry>()
                        .eq(ScheduleEntry::getScheduleId, schedule.getId())
                        .eq(result.getRelayTeamId() == null, ScheduleEntry::getRegistrationId, result.getRegistrationId())
                        .eq(result.getRelayTeamId() != null, ScheduleEntry::getRelayTeamId, result.getRelayTeamId()));
                if (matchingEntry == 0) throw new BusinessException("该参赛者/队伍不在当前赛程名单中");
            }
        }

        String resultStatus = result.getResultStatus();
        if (resultStatus == null || resultStatus.isBlank()) resultStatus = "VALID";
        if (!Set.of("VALID", "DNS", "DNF", "DQ", "WD").contains(resultStatus)) {
            throw new BusinessException("成绩状态无效");
        }
        result.setResultStatus(resultStatus);

        if (result.getRelayTeamId() != null) {
            if (result.getRegistrationId() != null) throw new BusinessException("接力成绩不能同时关联个人报名记录");
            RelayTeam team = relayTeamService.getById(result.getRelayTeamId());
            if (team == null || !Objects.equals(team.getStatus(), 2)
                    || !Objects.equals(team.getEventId(), result.getEventId())
                    || !Objects.equals(team.getSportItemId(), result.getSportItemId())) {
                throw new BusinessException("接力成绩必须关联同赛事、同项目且审核通过的接力队伍");
            }
            result.setUserId(null);
        } else {
            if (result.getRegistrationId() == null) throw new BusinessException("个人成绩必须关联报名记录");
            Registration registration = registrationService.getById(result.getRegistrationId());
            if (registration == null || !Objects.equals(registration.getStatus(), 2)) {
                throw new BusinessException("只能为已审核通过的个人报名录入成绩");
            }
            if (!Objects.equals(registration.getEventId(), result.getEventId())
                    || !Objects.equals(registration.getSportItemId(), result.getSportItemId())) {
                throw new BusinessException("成绩所属赛事或项目与报名记录不一致");
            }
            if (result.getUserId() != null && !Objects.equals(result.getUserId(), registration.getUserId())) {
                throw new BusinessException("成绩参赛者与报名记录不一致");
            }
            result.setUserId(registration.getUserId());
        }

        if ("VALID".equals(resultStatus)) {
            if (result.getScore() == null) throw new BusinessException("有效成绩必须填写成绩数值");
        } else {
            result.setScore(null);
            result.setRanking(null);
            result.setPoints(0);
        }
        result.setRecorderId(SecurityUtils.getUserId());
        result.setRecordTime(LocalDateTime.now());
        result.setReviewStatus(1);
        result.setReviewerId(null);
        result.setReviewTime(null);
        result.setReviewRemark(null);
        result.setIsPublished(0);
        result.setPublishedBy(null);
        result.setPublishedAt(null);
    }

    private void recalculateRankings(Long eventId, Long sportItemId, Long scheduleId) {
        if (eventId == null || sportItemId == null) return;
        SportItem item = sportItemService.getById(sportItemId);
        if (item == null) return;
        LambdaQueryWrapper<CompetitionResult> query = new LambdaQueryWrapper<CompetitionResult>()
                .eq(CompetitionResult::getEventId, eventId)
                .eq(CompetitionResult::getSportItemId, sportItemId)
                .eq(CompetitionResult::getResultStatus, "VALID")
                .isNotNull(CompetitionResult::getScore);
        if (scheduleId == null) query.isNull(CompetitionResult::getScheduleId);
        else query.eq(CompetitionResult::getScheduleId, scheduleId);
        List<CompetitionResult> ranked = this.list(query);
        boolean ascending = "ASC".equalsIgnoreCase(item.getSortRule())
                || (item.getSortRule() == null && "TIME".equalsIgnoreCase(item.getScoreType()));
        Comparator<CompetitionResult> comparator = Comparator.comparing(CompetitionResult::getScore);
        if (!ascending) comparator = comparator.reversed();
        ranked.sort(comparator);
        Integer previousRank = null;
        BigDecimal previousScore = null;
        for (int i = 0; i < ranked.size(); i++) {
            CompetitionResult current = ranked.get(i);
            if (previousScore != null && current.getScore().compareTo(previousScore) == 0) {
                current.setRanking(previousRank);
            } else {
                current.setRanking(i + 1);
                previousRank = i + 1;
                previousScore = current.getScore();
            }
        }
        this.updateBatchById(ranked);
    }

    private IPage<CompetitionResultVO> convertToVOPage(IPage<CompetitionResult> page) {
        Page<CompetitionResultVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(convertToVOList(page.getRecords()));
        return voPage;
    }

    private List<CompetitionResultVO> convertToVOList(List<CompetitionResult> results) {
        if (results == null || results.isEmpty()) return List.of();
        Set<Long> eventIds = results.stream().map(CompetitionResult::getEventId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> itemIds = results.stream().map(CompetitionResult::getSportItemId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = results.stream().map(CompetitionResult::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        results.stream().map(CompetitionResult::getRecorderId).filter(Objects::nonNull).forEach(userIds::add);
        results.stream().map(CompetitionResult::getReviewerId).filter(Objects::nonNull).forEach(userIds::add);
        Set<Long> teamIds = results.stream().map(CompetitionResult::getRelayTeamId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, Event> events = eventIds.isEmpty() ? Map.of() : eventService.listByIds(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, e -> e));
        Map<Long, SportItem> items = itemIds.isEmpty() ? Map.of() : sportItemService.listByIds(itemIds).stream()
                .collect(Collectors.toMap(SportItem::getId, e -> e));
        Map<Long, SysUser> users = userIds.isEmpty() ? Map.of() : sysUserService.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, e -> e));
        Map<Long, RelayTeam> teams = teamIds.isEmpty() ? Map.of() : relayTeamService.listByIds(teamIds).stream()
                .collect(Collectors.toMap(RelayTeam::getId, e -> e));

        return results.stream().map(result -> {
            CompetitionResultVO vo = new CompetitionResultVO();
            BeanUtils.copyProperties(result, vo);
            vo.setUnit(result.getScoreText());
            Event event = events.get(result.getEventId());
            if (event != null) vo.setEventName(event.getName());
            SportItem item = items.get(result.getSportItemId());
            if (item != null) vo.setSportItemName(item.getName());
            SysUser user = users.get(result.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setRealName(user.getRealName());
                vo.setStudentNo(user.getStudentNo());
                vo.setCollege(user.getCollege());
            }
            RelayTeam team = teams.get(result.getRelayTeamId());
            if (team != null) {
                vo.setRelayTeamName(team.getTeamName());
                vo.setCollege(team.getCollege());
            }
            SysUser recorder = users.get(result.getRecorderId());
            if (recorder != null) vo.setRecorderName(recorder.getRealName());
            SysUser reviewer = users.get(result.getReviewerId());
            if (reviewer != null) vo.setReviewerName(reviewer.getRealName());
            return vo;
        }).collect(Collectors.toList());
    }

    private String participantKey(CompetitionResult result) {
        return result.getRelayTeamId() != null ? "T:" + result.getRelayTeamId() : "R:" + result.getRegistrationId();
    }

    private CompetitionResult copyResult(CompetitionResult source) {
        CompetitionResult copy = new CompetitionResult();
        BeanUtils.copyProperties(source, copy);
        return copy;
    }

    private void writeAudit(CompetitionResult result, String action, CompetitionResult before,
                            CompetitionResult after, String remark) {
        if (result == null || result.getId() == null) return;
        ResultAuditLog audit = new ResultAuditLog();
        audit.setResultId(result.getId());
        audit.setAction(action);
        audit.setBeforeValue(before == null ? null : snapshot(before));
        audit.setAfterValue(after == null ? null : snapshot(after));
        audit.setOperatorId(SecurityUtils.getUserId());
        audit.setOperatedAt(LocalDateTime.now());
        audit.setRemark(remark);
        resultAuditLogMapper.insert(audit);
    }

    private String snapshot(CompetitionResult result) {
        return "{\"eventId\":" + jsonNumber(result.getEventId())
                + ",\"sportItemId\":" + jsonNumber(result.getSportItemId())
                + ",\"scheduleId\":" + jsonNumber(result.getScheduleId())
                + ",\"registrationId\":" + jsonNumber(result.getRegistrationId())
                + ",\"relayTeamId\":" + jsonNumber(result.getRelayTeamId())
                + ",\"userId\":" + jsonNumber(result.getUserId())
                + ",\"score\":" + (result.getScore() == null ? "null" : result.getScore().toPlainString())
                + ",\"scoreText\":" + jsonString(result.getScoreText())
                + ",\"resultStatus\":" + jsonString(result.getResultStatus())
                + ",\"ranking\":" + jsonNumber(result.getRanking())
                + ",\"points\":" + jsonNumber(result.getPoints())
                + ",\"reviewStatus\":" + jsonNumber(result.getReviewStatus())
                + ",\"isPublished\":" + jsonNumber(result.getIsPublished()) + "}";
    }

    private String jsonNumber(Number value) {
        return value == null ? "null" : value.toString();
    }

    private String jsonString(String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
}
