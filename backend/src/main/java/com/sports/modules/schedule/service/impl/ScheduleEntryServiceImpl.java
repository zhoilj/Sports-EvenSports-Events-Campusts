package com.sports.modules.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.common.exception.BusinessException;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.service.EventService;
import com.sports.modules.registration.entity.Registration;
import com.sports.modules.registration.entity.RelayTeam;
import com.sports.modules.registration.entity.RelayTeamMember;
import com.sports.modules.registration.mapper.RelayTeamMapper;
import com.sports.modules.registration.mapper.RelayTeamMemberMapper;
import com.sports.modules.registration.service.RegistrationService;
import com.sports.modules.schedule.dto.CheckinUpdateDTO;
import com.sports.modules.schedule.dto.ScheduleEntryAssignmentDTO;
import com.sports.modules.schedule.entity.CheckinRecord;
import com.sports.modules.schedule.entity.Schedule;
import com.sports.modules.schedule.entity.ScheduleEntry;
import com.sports.modules.schedule.mapper.CheckinRecordMapper;
import com.sports.modules.schedule.mapper.ScheduleEntryMapper;
import com.sports.modules.schedule.service.ScheduleEntryService;
import com.sports.modules.schedule.service.ScheduleService;
import com.sports.modules.schedule.vo.ScheduleEntryVO;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.service.SportItemService;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.service.SysUserService;
import com.sports.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleEntryServiceImpl extends ServiceImpl<ScheduleEntryMapper, ScheduleEntry>
        implements ScheduleEntryService {

    private static final int SCHEDULE_PENDING = 1;
    private static final int SCHEDULE_COMPLETED = 3;
    private static final int SCHEDULE_CANCELLED = 4;
    private static final Set<String> CHECKIN_STATUSES =
            Set.of("CHECKED_IN", "ABSENT", "WITHDRAWN", "DQ");

    private final ScheduleService scheduleService;
    private final EventService eventService;
    private final SportItemService sportItemService;
    private final RegistrationService registrationService;
    private final RelayTeamMapper relayTeamMapper;
    private final RelayTeamMemberMapper relayTeamMemberMapper;
    private final CheckinRecordMapper checkinRecordMapper;
    private final SysUserService sysUserService;

    @Override
    public List<ScheduleEntryVO> listByScheduleId(Long scheduleId) {
        requireSchedule(scheduleId);
        List<ScheduleEntry> entries = this.list(new LambdaQueryWrapper<ScheduleEntry>()
                .eq(ScheduleEntry::getScheduleId, scheduleId)
                .orderByAsc(ScheduleEntry::getGroupNo)
                .orderByAsc(ScheduleEntry::getLaneNo)
                .orderByAsc(ScheduleEntry::getStartOrder)
                .orderByAsc(ScheduleEntry::getId));
        return entries.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<ScheduleEntryVO> listMyEntries() {
        Long userId = SecurityUtils.getUserId();
        List<Long> registrationIds = registrationService.list(new LambdaQueryWrapper<Registration>()
                        .eq(Registration::getUserId, userId)
                        .eq(Registration::getStatus, 2))
                .stream().map(Registration::getId).collect(Collectors.toList());

        List<Long> teamIds = relayTeamMemberMapper.selectList(new LambdaQueryWrapper<RelayTeamMember>()
                        .eq(RelayTeamMember::getUserId, userId))
                .stream().map(RelayTeamMember::getTeamId).distinct().collect(Collectors.toList());
        if (!teamIds.isEmpty()) {
            teamIds = relayTeamMapper.selectList(new LambdaQueryWrapper<RelayTeam>()
                            .in(RelayTeam::getId, teamIds)
                            .eq(RelayTeam::getStatus, 2))
                    .stream().map(RelayTeam::getId).collect(Collectors.toList());
        }

        if (registrationIds.isEmpty() && teamIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<ScheduleEntry> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(condition -> {
            if (!registrationIds.isEmpty()) {
                condition.in(ScheduleEntry::getRegistrationId, registrationIds);
            }
            if (!teamIds.isEmpty()) {
                if (!registrationIds.isEmpty()) {
                    condition.or();
                }
                condition.in(ScheduleEntry::getRelayTeamId, teamIds);
            }
        }).orderByAsc(ScheduleEntry::getId);

        return this.list(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoAssign(Long scheduleId, Integer laneCount) {
        Schedule schedule = scheduleService.getOne(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getId, scheduleId)
                .last("FOR UPDATE"));
        if (schedule == null) {
            throw new BusinessException("未找到对应的赛程");
        }
        if (!Objects.equals(schedule.getStatus(), SCHEDULE_PENDING)) {
            throw new BusinessException("仅未开始的场次可以进行编排");
        }
        long existing = this.count(new LambdaQueryWrapper<ScheduleEntry>()
                .eq(ScheduleEntry::getScheduleId, scheduleId));
        if (existing > 0) {
            throw new BusinessException("该场次已有编排记录，请先移除原记录后再重新编排");
        }

        SportItem item = sportItemService.getById(schedule.getSportItemId());
        if (item == null || !Objects.equals(item.getEventId(), schedule.getEventId())) {
            throw new BusinessException("赛程关联的运动项目不存在或不属于该赛事");
        }

        boolean relay = Objects.equals(item.getIsRelay(), 1);
        List<Long> participantIds;
        if (relay) {
            participantIds = relayTeamMapper.selectList(new LambdaQueryWrapper<RelayTeam>()
                            .eq(RelayTeam::getEventId, schedule.getEventId())
                            .eq(RelayTeam::getSportItemId, schedule.getSportItemId())
                            .eq(RelayTeam::getStatus, 2)
                            .orderByAsc(RelayTeam::getCreateTime)
                            .orderByAsc(RelayTeam::getId))
                    .stream().map(RelayTeam::getId).collect(Collectors.toList());
        } else {
            participantIds = registrationService.list(new LambdaQueryWrapper<Registration>()
                            .eq(Registration::getEventId, schedule.getEventId())
                            .eq(Registration::getSportItemId, schedule.getSportItemId())
                            .eq(Registration::getStatus, 2)
                            .orderByAsc(Registration::getCreateTime)
                            .orderByAsc(Registration::getId))
                    .stream().map(Registration::getId).collect(Collectors.toList());
        }

        if (participantIds.isEmpty()) {
            throw new BusinessException("当前项目没有审核通过的参赛者，无法编排");
        }

        boolean track = "TRACK".equalsIgnoreCase(item.getType());
        int effectiveLaneCount = laneCount == null ? 8 : laneCount;
        if (track && (effectiveLaneCount < 1 || effectiveLaneCount > 12)) {
            throw new BusinessException("径赛跑道数应设置为1至12道");
        }

        List<ScheduleEntry> entries = new ArrayList<>(participantIds.size());
        for (int i = 0; i < participantIds.size(); i++) {
            ScheduleEntry entry = new ScheduleEntry();
            entry.setScheduleId(scheduleId);
            if (relay) {
                entry.setRelayTeamId(participantIds.get(i));
            } else {
                entry.setRegistrationId(participantIds.get(i));
            }
            entry.setQualificationStatus("ENTERED");
            if (track) {
                entry.setGroupNo(groupName(i / effectiveLaneCount));
                entry.setLaneNo(i % effectiveLaneCount + 1);
            } else {
                entry.setGroupNo(StringUtils.hasText(schedule.getGroupNo()) ? schedule.getGroupNo() : "A组");
                entry.setLaneNo(null);
            }
            entry.setStartOrder(i + 1);
            entries.add(entry);
        }

        for (ScheduleEntry entry : entries) {
            if (!this.save(entry)) {
                throw new BusinessException("保存竞赛编排记录失败");
            }
            CheckinRecord checkin = new CheckinRecord();
            checkin.setScheduleEntryId(entry.getId());
            checkin.setStatus("PENDING");
            if (checkinRecordMapper.insert(checkin) != 1) {
                throw new BusinessException("初始化检录记录失败");
            }
        }
        return entries.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAssignment(Long id, ScheduleEntryAssignmentDTO dto) {
        if (dto == null) {
            throw new BusinessException("编排信息不能为空");
        }
        ScheduleEntry entry = this.getById(id);
        if (entry == null) {
            throw new BusinessException("未找到对应的参赛编排记录");
        }
        Schedule schedule = requireSchedule(entry.getScheduleId());
        if (!Objects.equals(schedule.getStatus(), SCHEDULE_PENDING)) {
            throw new BusinessException("比赛开始后不能调整分组和跑道");
        }
        if (dto.getStartOrder() == null || dto.getStartOrder() < 1) {
            throw new BusinessException("出场顺序必须大于0");
        }

        SportItem item = sportItemService.getById(schedule.getSportItemId());
        if (item != null && "TRACK".equalsIgnoreCase(item.getType())) {
            if (dto.getLaneNo() == null || dto.getLaneNo() < 1 || dto.getLaneNo() > 12) {
                throw new BusinessException("径赛跑道号应为1至12");
            }
        }
        String groupNo = StringUtils.hasText(dto.getGroupNo()) ? dto.getGroupNo().trim() : entry.getGroupNo();
        if (!StringUtils.hasText(groupNo)) {
            throw new BusinessException("组别不能为空");
        }
        long duplicateOrder = this.count(new LambdaQueryWrapper<ScheduleEntry>()
                .eq(ScheduleEntry::getScheduleId, entry.getScheduleId())
                .eq(ScheduleEntry::getGroupNo, groupNo)
                .eq(ScheduleEntry::getStartOrder, dto.getStartOrder())
                .ne(ScheduleEntry::getId, id));
        if (duplicateOrder > 0) {
            throw new BusinessException("同一组别的出场顺序不能重复");
        }
        if (item != null && "TRACK".equalsIgnoreCase(item.getType())) {
            long duplicateLane = this.count(new LambdaQueryWrapper<ScheduleEntry>()
                    .eq(ScheduleEntry::getScheduleId, entry.getScheduleId())
                    .eq(ScheduleEntry::getGroupNo, groupNo)
                    .eq(ScheduleEntry::getLaneNo, dto.getLaneNo())
                    .ne(ScheduleEntry::getId, id));
            if (duplicateLane > 0) {
                throw new BusinessException("该组别的跑道已分配给其他参赛者");
            }
        }
        entry.setGroupNo(groupNo);
        entry.setLaneNo(dto.getLaneNo());
        entry.setStartOrder(dto.getStartOrder());
        this.updateById(entry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkIn(Long id, CheckinUpdateDTO dto) {
        if (dto == null) {
            throw new BusinessException("检录信息不能为空");
        }
        ScheduleEntry entry = this.getById(id);
        if (entry == null) {
            throw new BusinessException("未找到对应的参赛编排记录");
        }
        Schedule schedule = requireSchedule(entry.getScheduleId());
        if (Objects.equals(schedule.getStatus(), SCHEDULE_COMPLETED)
                || Objects.equals(schedule.getStatus(), SCHEDULE_CANCELLED)) {
            throw new BusinessException("比赛已结束或已取消，不能修改检录状态");
        }
        String status = normalizeCheckinStatus(dto.getStatus());
        CheckinRecord record = checkinRecordMapper.selectOne(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getScheduleEntryId, id));
        if (record == null) {
            record = new CheckinRecord();
            record.setScheduleEntryId(id);
            record.setStatus(status);
        } else {
            record.setStatus(status);
        }
        record.setCheckedBy(SecurityUtils.getUserId());
        record.setCheckedAt(LocalDateTime.now());
        record.setRemark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : null);
        if (record.getId() == null) {
            if (checkinRecordMapper.insert(record) != 1) {
                throw new BusinessException("保存检录记录失败");
            }
        } else if (checkinRecordMapper.updateById(record) != 1) {
            throw new BusinessException("更新检录记录失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeEntry(Long id) {
        ScheduleEntry entry = this.getById(id);
        if (entry == null) {
            throw new BusinessException("未找到对应的参赛编排记录");
        }
        Schedule schedule = requireSchedule(entry.getScheduleId());
        if (!Objects.equals(schedule.getStatus(), SCHEDULE_PENDING)) {
            throw new BusinessException("比赛开始后不能删除编排记录");
        }
        CheckinRecord record = checkinRecordMapper.selectOne(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getScheduleEntryId, id));
        if (record != null && !"PENDING".equalsIgnoreCase(record.getStatus())) {
            throw new BusinessException("已有检录结果，不能删除该参赛记录");
        }
        if (record != null) {
            checkinRecordMapper.deleteById(record.getId());
        }
        this.removeById(id);
    }

    private Schedule requireSchedule(Long scheduleId) {
        Schedule schedule = scheduleService.getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("未找到对应的赛程");
        }
        return schedule;
    }

    private ScheduleEntryVO toVO(ScheduleEntry entry) {
        ScheduleEntryVO vo = new ScheduleEntryVO();
        BeanUtils.copyProperties(entry, vo);
        vo.setCheckinCode("CK" + String.format(Locale.ROOT, "%08d", entry.getId()));

        Schedule schedule = scheduleService.getById(entry.getScheduleId());
        if (schedule != null) {
            vo.setEventId(schedule.getEventId());
            vo.setSportItemId(schedule.getSportItemId());
            vo.setScheduleName(schedule.getScheduleName());
            vo.setMatchDate(schedule.getMatchDate());
            vo.setStartTime(schedule.getStartTime());
            vo.setLocation(schedule.getLocation());

            Event event = eventService.getById(schedule.getEventId());
            if (event != null) {
                vo.setEventName(event.getName());
            }
            SportItem item = sportItemService.getById(schedule.getSportItemId());
            if (item != null) {
                vo.setSportItemName(item.getName());
            }
        }

        if (entry.getRegistrationId() != null) {
            vo.setParticipantType("INDIVIDUAL");
            Registration registration = registrationService.getById(entry.getRegistrationId());
            if (registration != null) {
                SysUser user = sysUserService.getById(registration.getUserId());
                if (user != null) {
                    vo.setParticipantName(user.getRealName());
                    vo.setStudentNo(user.getStudentNo());
                    vo.setCollege(user.getCollege());
                    vo.setClassName(user.getClassName());
                }
            }
        } else if (entry.getRelayTeamId() != null) {
            vo.setParticipantType("RELAY");
            RelayTeam team = relayTeamMapper.selectById(entry.getRelayTeamId());
            if (team != null) {
                vo.setTeamName(team.getTeamName());
                vo.setParticipantName(team.getTeamName());
                vo.setCollege(team.getCollege());
                List<RelayTeamMember> members = relayTeamMemberMapper.selectList(
                        new LambdaQueryWrapper<RelayTeamMember>()
                                .eq(RelayTeamMember::getTeamId, team.getId())
                                .orderByAsc(RelayTeamMember::getReserve)
                                .orderByAsc(RelayTeamMember::getLegOrder));
                List<Long> memberIds = members.stream().map(RelayTeamMember::getUserId).collect(Collectors.toList());
                if (!memberIds.isEmpty()) {
                    vo.setMemberNames(sysUserService.listByIds(memberIds).stream()
                            .sorted(Comparator.comparing(user -> memberIds.indexOf(user.getId())))
                            .map(user -> StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getStudentNo())
                            .collect(Collectors.toList()));
                } else {
                    vo.setMemberNames(List.of());
                }
            }
        }

        CheckinRecord checkin = checkinRecordMapper.selectOne(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getScheduleEntryId, entry.getId()));
        vo.setCheckinStatus(checkin == null || !StringUtils.hasText(checkin.getStatus()) ? "PENDING" : checkin.getStatus());
        if (checkin != null) {
            vo.setCheckedAt(checkin.getCheckedAt());
            vo.setCheckinRemark(checkin.getRemark());
        }
        return vo;
    }

    static String groupName(int zeroBasedIndex) {
        if (zeroBasedIndex >= 0 && zeroBasedIndex < 26) {
            return String.valueOf((char) ('A' + zeroBasedIndex)) + "组";
        }
        return "第" + (zeroBasedIndex + 1) + "组";
    }

    static String normalizeCheckinStatus(String rawStatus) {
        String status = rawStatus == null ? "" : rawStatus.trim().toUpperCase(Locale.ROOT);
        if (!CHECKIN_STATUSES.contains(status)) {
            throw new BusinessException("不支持的检录状态，请选择已检录、缺席、弃权或取消资格");
        }
        return status;
    }
}
