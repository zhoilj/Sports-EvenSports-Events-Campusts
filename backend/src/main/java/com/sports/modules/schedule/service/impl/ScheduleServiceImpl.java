package com.sports.modules.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.ResultCode;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.service.EventService;
import com.sports.modules.schedule.entity.Schedule;
import com.sports.modules.schedule.mapper.ScheduleMapper;
import com.sports.modules.schedule.service.ScheduleService;
import com.sports.modules.schedule.vo.ScheduleVO;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.service.SportItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 赛程 Service 实现
 *
 * @author sports
 */
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule> implements ScheduleService {

    private final EventService eventService;
    private final SportItemService sportItemService;

    // 状态常量: 1-未开始 2-进行中 3-已结束
    private static final Integer STATUS_PENDING = 1;
    private static final Integer STATUS_ONGOING = 2;
    private static final Integer STATUS_COMPLETED = 3;

    @Override
    public IPage<ScheduleVO> pageSchedulesVO(Integer current, Integer size, Long eventId, String matchDate, Long sportItemId) {
        IPage<Schedule> schedulePage = pageSchedules(current, size, eventId, matchDate, sportItemId);

        return schedulePage.convert(this::convertToVO);
    }

    private ScheduleVO convertToVO(Schedule schedule) {
        ScheduleVO vo = new ScheduleVO();
        BeanUtils.copyProperties(schedule, vo);

        // 获取赛事名称
        if (schedule.getEventId() != null) {
            Event event = eventService.getById(schedule.getEventId());
            if (event != null) {
                vo.setEventName(event.getName());
            }
        }

        // 获取运动项目名称
        if (schedule.getSportItemId() != null) {
            SportItem sportItem = sportItemService.getById(schedule.getSportItemId());
            if (sportItem != null) {
                vo.setSportItemName(sportItem.getName());
                vo.setCompetitionGroup(sportItem.getCompetitionGroup());
            }
        }

        return vo;
    }

    @Override
    public IPage<Schedule> pageSchedules(Integer current, Integer size, Long eventId, String matchDate, Long sportItemId) {
        Page<Schedule> page = new Page<>(current, size);

        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(eventId != null, Schedule::getEventId, eventId)
                .eq(sportItemId != null, Schedule::getSportItemId, sportItemId)
                .eq(StringUtils.hasText(matchDate), Schedule::getMatchDate, matchDate != null ? LocalDate.parse(matchDate) : null)
                .orderByAsc(Schedule::getMatchDate)
                .orderByAsc(Schedule::getStartTime);

        return this.page(page, wrapper);
    }

    @Override
    public void validateSchedule(Schedule schedule, Long excludeId) {
        if (schedule == null || schedule.getEventId() == null || schedule.getSportItemId() == null) {
            throw new BusinessException("赛事和比赛项目不能为空");
        }
        if (schedule.getMatchDate() == null || schedule.getStartTime() == null) {
            throw new BusinessException("比赛日期和开始时间不能为空");
        }
        if (schedule.getEndTime() != null && !schedule.getEndTime().isAfter(schedule.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }

        Event event = eventService.getById(schedule.getEventId());
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }
        if (event.getStartDate() != null && schedule.getMatchDate().isBefore(event.getStartDate())
                || event.getEndDate() != null && schedule.getMatchDate().isAfter(event.getEndDate())) {
            throw new BusinessException("比赛日期必须位于赛事开始和结束日期之间");
        }

        SportItem item = sportItemService.getById(schedule.getSportItemId());
        if (item == null || !java.util.Objects.equals(item.getEventId(), schedule.getEventId())) {
            throw new BusinessException("所选比赛项目不属于当前赛事");
        }

        String roundType = schedule.getRoundType();
        if (roundType != null && !java.util.Set.of("PRELIMINARY", "SEMIFINAL", "FINAL", "FIELD_FINAL").contains(roundType)) {
            throw new BusinessException("比赛轮次类型无效");
        }

        java.time.LocalTime proposedEnd = schedule.getEndTime() == null
                ? schedule.getStartTime().plusHours(1) : schedule.getEndTime();
        List<Schedule> sameDay = this.list(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getMatchDate, schedule.getMatchDate())
                .ne(excludeId != null, Schedule::getId, excludeId)
                .ne(Schedule::getStatus, 4));
        for (Schedule existing : sameDay) {
            if (!StringUtils.hasText(schedule.getLocation()) || !StringUtils.hasText(existing.getLocation())
                    || !schedule.getLocation().trim().equalsIgnoreCase(existing.getLocation().trim())) {
                continue;
            }
            java.time.LocalTime existingEnd = existing.getEndTime() == null
                    ? existing.getStartTime().plusHours(1) : existing.getEndTime();
            boolean overlaps = schedule.getStartTime().isBefore(existingEnd)
                    && existing.getStartTime().isBefore(proposedEnd);
            if (overlaps) {
                throw new BusinessException("场地时间冲突：场地“" + schedule.getLocation() + "”在"
                        + existing.getStartTime() + "—" + existingEnd + "已有赛程“"
                        + existing.getScheduleName() + "”（赛程ID " + existing.getId() + "）");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startMatch(Long id) {
        Schedule schedule = this.getById(id);
        if (schedule == null) {
            throw new BusinessException(ResultCode.SCHEDULE_NOT_FOUND);
        }
        if (!STATUS_PENDING.equals(schedule.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        schedule.setStatus(STATUS_ONGOING);
        this.updateById(schedule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeMatch(Long id) {
        Schedule schedule = this.getById(id);
        if (schedule == null) {
            throw new BusinessException(ResultCode.SCHEDULE_NOT_FOUND);
        }
        if (!STATUS_ONGOING.equals(schedule.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        schedule.setStatus(STATUS_COMPLETED);
        this.updateById(schedule);
    }
}
