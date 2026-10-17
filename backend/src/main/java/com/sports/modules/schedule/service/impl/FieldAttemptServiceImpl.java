package com.sports.modules.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.common.exception.BusinessException;
import com.sports.modules.registration.entity.Registration;
import com.sports.modules.registration.service.RegistrationService;
import com.sports.modules.result.entity.CompetitionResult;
import com.sports.modules.result.service.CompetitionResultService;
import com.sports.modules.schedule.dto.FieldAttemptDTO;
import com.sports.modules.schedule.entity.CheckinRecord;
import com.sports.modules.schedule.entity.FieldAttempt;
import com.sports.modules.schedule.entity.Schedule;
import com.sports.modules.schedule.entity.ScheduleEntry;
import com.sports.modules.schedule.mapper.CheckinRecordMapper;
import com.sports.modules.schedule.mapper.FieldAttemptMapper;
import com.sports.modules.schedule.mapper.ScheduleEntryMapper;
import com.sports.modules.schedule.service.FieldAttemptService;
import com.sports.modules.schedule.service.ScheduleService;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.service.SportItemService;
import com.sports.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FieldAttemptServiceImpl extends ServiceImpl<FieldAttemptMapper, FieldAttempt>
        implements FieldAttemptService {

    private final ScheduleService scheduleService;
    private final ScheduleEntryMapper scheduleEntryMapper;
    private final CheckinRecordMapper checkinRecordMapper;
    private final SportItemService sportItemService;
    private final RegistrationService registrationService;
    private final CompetitionResultService competitionResultService;

    @Override
    public List<FieldAttempt> listBySchedule(Long scheduleId) {
        List<Long> entryIds = scheduleEntryMapper.selectList(new LambdaQueryWrapper<ScheduleEntry>()
                        .eq(ScheduleEntry::getScheduleId, scheduleId))
                .stream().map(ScheduleEntry::getId).collect(Collectors.toList());
        if (entryIds.isEmpty()) return List.of();
        return this.list(new LambdaQueryWrapper<FieldAttempt>()
                .in(FieldAttempt::getScheduleEntryId, entryIds)
                .orderByAsc(FieldAttempt::getScheduleEntryId)
                .orderByAsc(FieldAttempt::getAttemptNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAttempts(List<FieldAttemptDTO> attempts) {
        if (attempts == null || attempts.isEmpty()) throw new BusinessException("请提交至少一条试跳/试投记录");
        Long scheduleId = null;
        List<FieldAttempt> toSave = new ArrayList<>();
        for (FieldAttemptDTO dto : attempts) {
            if (dto == null || dto.getScheduleEntryId() == null || dto.getAttemptNo() == null) {
                throw new BusinessException("参赛名单和试跳/试投轮次不能为空");
            }
            ScheduleEntry entry = scheduleEntryMapper.selectById(dto.getScheduleEntryId());
            if (entry == null) throw new BusinessException("参赛名单记录不存在");
            if (scheduleId == null) scheduleId = entry.getScheduleId();
            if (!Objects.equals(scheduleId, entry.getScheduleId())) {
                throw new BusinessException("一次提交的试跳/试投记录必须属于同一赛程");
            }
            Schedule schedule = scheduleService.getById(entry.getScheduleId());
            validateFieldSchedule(schedule);
            if (schedule.getStatus() != 2) throw new BusinessException("只有进行中的田赛赛程可以录入试跳/试投成绩");
            CheckinRecord checkin = checkinRecordMapper.selectOne(new LambdaQueryWrapper<CheckinRecord>()
                    .eq(CheckinRecord::getScheduleEntryId, entry.getId()));
            if (checkin == null || !"CHECKED_IN".equals(checkin.getStatus())) {
                throw new BusinessException("只有完成检录的参赛者才能录入试跳/试投成绩");
            }
            if (dto.getAttemptNo() < 1 || dto.getAttemptNo() > 6) {
                throw new BusinessException("试跳/试投轮次必须在1至6轮之间");
            }
            boolean foul = Boolean.TRUE.equals(dto.getFoul());
            if (!foul && (dto.getScore() == null || dto.getScore().compareTo(BigDecimal.ZERO) < 0)) {
                throw new BusinessException("有效试跳/试投必须填写非负成绩");
            }

            FieldAttempt existing = dto.getId() == null
                    ? this.getOne(new LambdaQueryWrapper<FieldAttempt>()
                        .eq(FieldAttempt::getScheduleEntryId, entry.getId())
                        .eq(FieldAttempt::getAttemptNo, dto.getAttemptNo()), false)
                    : this.getById(dto.getId());
            if (existing != null && !Objects.equals(existing.getScheduleEntryId(), entry.getId())) {
                throw new BusinessException("试跳/试投记录与参赛名单不匹配");
            }
            FieldAttempt record = existing == null ? new FieldAttempt() : existing;
            record.setScheduleEntryId(entry.getId());
            record.setAttemptNo(dto.getAttemptNo());
            record.setIsFoul(foul ? 1 : 0);
            record.setIsValid(foul ? 0 : 1);
            record.setScore(foul ? null : dto.getScore());
            record.setRemark(dto.getRemark());
            record.setRecorderId(SecurityUtils.getUserId());
            record.setRecordedAt(LocalDateTime.now());
            toSave.add(record);
        }
        for (FieldAttempt record : toSave) this.saveOrUpdate(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int finalizeResults(Long scheduleId) {
        Schedule schedule = scheduleService.getById(scheduleId);
        validateFieldSchedule(schedule);
        if (schedule.getStatus() == null || schedule.getStatus() != 3) {
            throw new BusinessException("请先结束田赛赛程，再汇总正式成绩");
        }
        List<ScheduleEntry> entries = scheduleEntryMapper.selectList(new LambdaQueryWrapper<ScheduleEntry>()
                .eq(ScheduleEntry::getScheduleId, scheduleId));
        if (entries.isEmpty()) throw new BusinessException("该赛程尚未生成参赛名单");
        SportItem item = sportItemService.getById(schedule.getSportItemId());
        boolean ascending = "TIME".equalsIgnoreCase(item.getScoreType())
                || "ASC".equalsIgnoreCase(item.getSortRule());
        int finalized = 0;
        for (ScheduleEntry entry : entries) {
            CheckinRecord checkin = checkinRecordMapper.selectOne(new LambdaQueryWrapper<CheckinRecord>()
                    .eq(CheckinRecord::getScheduleEntryId, entry.getId()));
            if (checkin != null && Set.of("ABSENT", "WITHDRAWN", "DQ").contains(checkin.getStatus())) continue;
            if (checkin == null || !"CHECKED_IN".equals(checkin.getStatus())) {
                throw new BusinessException("仍有参赛者未完成检录或未登记终止状态，不能汇总田赛成绩");
            }
            List<FieldAttempt> attempts = this.list(new LambdaQueryWrapper<FieldAttempt>()
                    .eq(FieldAttempt::getScheduleEntryId, entry.getId())
                    .orderByAsc(FieldAttempt::getAttemptNo));
            if (attempts.isEmpty()) throw new BusinessException("仍有已检录参赛者未录入任何试跳/试投记录");
            List<FieldAttempt> valid = attempts.stream()
                    .filter(a -> Objects.equals(a.getIsValid(), 1) && a.getScore() != null)
                    .collect(Collectors.toList());

            CompetitionResult result = new CompetitionResult();
            result.setEventId(schedule.getEventId());
            result.setSportItemId(schedule.getSportItemId());
            result.setScheduleId(scheduleId);
            result.setRegistrationId(entry.getRegistrationId());
            if (entry.getRegistrationId() == null) {
                throw new BusinessException("田赛参赛名单必须关联个人报名记录");
            }
            Registration registration = registrationService.getById(entry.getRegistrationId());
            if (registration == null) throw new BusinessException("参赛报名记录不存在");
            result.setUserId(registration.getUserId());

            if (valid.isEmpty()) {
                result.setResultStatus("DQ");
                result.setScoreText("无有效试跳/试投");
                result.setRemark("所有已登记试跳/试投均无效");
            } else {
                BigDecimal bestScore = valid.stream().map(FieldAttempt::getScore)
                        .reduce((left, right) -> ascending
                                ? (left.compareTo(right) <= 0 ? left : right)
                                : (left.compareTo(right) >= 0 ? left : right)).orElseThrow();
                result.setResultStatus("VALID");
                result.setScore(bestScore);
                result.setScoreText(bestScore.stripTrailingZeros().toPlainString()
                        + (item.getScoreUnit() == null ? "" : item.getScoreUnit()));
                result.setRemark("田赛最佳有效成绩");
            }
            competitionResultService.saveValidatedResult(result);
            finalized++;
        }
        return finalized;
    }

    private void validateFieldSchedule(Schedule schedule) {
        if (schedule == null) throw new BusinessException("赛程不存在");
        SportItem item = sportItemService.getById(schedule.getSportItemId());
        if (item == null || !"FIELD".equalsIgnoreCase(item.getType())) {
            throw new BusinessException("试跳/试投功能仅适用于田赛项目");
        }
        if (schedule.getStatus() == null || (schedule.getStatus() != 2 && schedule.getStatus() != 3)) {
            throw new BusinessException("田赛赛程必须处于进行中或已结束状态");
        }
    }
}
