package com.sports.modules.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.common.exception.BusinessException;
import com.sports.modules.registration.entity.Registration;
import com.sports.modules.registration.entity.RelayTeam;
import com.sports.modules.registration.service.RegistrationService;
import com.sports.modules.registration.service.RelayTeamService;
import com.sports.modules.result.entity.CompetitionResult;
import com.sports.modules.result.service.CompetitionResultService;
import com.sports.modules.schedule.dto.CheckinDTO;
import com.sports.modules.schedule.entity.CheckinRecord;
import com.sports.modules.schedule.entity.Schedule;
import com.sports.modules.schedule.entity.ScheduleEntry;
import com.sports.modules.schedule.mapper.CheckinRecordMapper;
import com.sports.modules.schedule.mapper.ScheduleEntryMapper;
import com.sports.modules.schedule.service.CheckinRecordService;
import com.sports.modules.schedule.service.ScheduleEntryService;
import com.sports.modules.schedule.service.ScheduleService;
import com.sports.modules.schedule.vo.ScheduleEntryVO;
import com.sports.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CheckinRecordServiceImpl extends ServiceImpl<CheckinRecordMapper, CheckinRecord>
        implements CheckinRecordService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("CHECKED_IN", "ABSENT", "WITHDRAWN", "DQ");

    private final ScheduleEntryMapper scheduleEntryMapper;
    private final ScheduleEntryService scheduleEntryService;
    private final ScheduleService scheduleService;
    private final RegistrationService registrationService;
    private final RelayTeamService relayTeamService;
    private final CompetitionResultService competitionResultService;

    @Override
    public List<ScheduleEntryVO> listBySchedule(Long scheduleId) {
        return scheduleEntryService.listEntryDetails(scheduleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long scheduleEntryId, String status, String remark) {
        if (status == null || !ALLOWED_STATUSES.contains(status)) {
            throw new BusinessException("检录状态必须为已检录、缺席、弃权或犯规取消");
        }
        ScheduleEntry entry = scheduleEntryMapper.selectById(scheduleEntryId);
        if (entry == null) throw new BusinessException("参赛名单记录不存在");
        Schedule schedule = scheduleService.getById(entry.getScheduleId());
        if (schedule == null) throw new BusinessException("赛程不存在");
        if (schedule.getStatus() == null || (schedule.getStatus() != 1 && schedule.getStatus() != 2)) {
            throw new BusinessException("赛程结束后不能修改检录状态");
        }

        CheckinRecord record = this.getOne(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getScheduleEntryId, scheduleEntryId), false);
        if (record == null) {
            record = new CheckinRecord();
            record.setScheduleEntryId(scheduleEntryId);
            record.setStatus("PENDING");
        }
        String oldStatus = record.getStatus();
        if (Set.of("ABSENT", "WITHDRAWN", "DQ").contains(oldStatus) && !oldStatus.equals(status)) {
            throw new BusinessException("缺席、弃权或取消资格属于终止状态，不能直接改回其他状态");
        }
        if ("PENDING".equals(oldStatus) && "WITHDRAWN".equals(status)) {
            // Allowed when an athlete withdraws before checking in.
        }
        record.setStatus(status);
        record.setCheckedBy(SecurityUtils.getUserId());
        record.setCheckedAt(LocalDateTime.now());
        record.setRemark(remark);
        if (record.getId() == null) this.save(record);
        else this.updateById(record);

        if (Set.of("ABSENT", "WITHDRAWN", "DQ").contains(status)) {
            CompetitionResult result = new CompetitionResult();
            result.setEventId(schedule.getEventId());
            result.setSportItemId(schedule.getSportItemId());
            result.setScheduleId(schedule.getId());
            result.setResultStatus("ABSENT".equals(status) ? "DNS" : ("WITHDRAWN".equals(status) ? "WD" : "DQ"));
            result.setScoreText("ABSENT".equals(status) ? "未出赛" : ("WITHDRAWN".equals(status) ? "弃权" : "取消资格"));
            result.setRemark(remark);
            if (entry.getRelayTeamId() != null) {
                RelayTeam team = relayTeamService.getById(entry.getRelayTeamId());
                if (team == null || !Objects.equals(team.getStatus(), 2)) {
                    throw new BusinessException("接力队伍尚未审核通过，不能登记检录结果");
                }
                result.setRelayTeamId(team.getId());
            } else {
                Registration registration = registrationService.getById(entry.getRegistrationId());
                if (registration == null || !Objects.equals(registration.getStatus(), 2)) {
                    throw new BusinessException("报名记录不存在或尚未审核通过");
                }
                result.setRegistrationId(registration.getId());
                result.setUserId(registration.getUserId());
            }
            competitionResultService.saveValidatedResult(result);
        }
    }
}
