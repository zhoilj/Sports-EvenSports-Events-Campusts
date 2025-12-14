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
    public IPage<ScheduleVO> pageSchedulesVO(Integer current, Integer size, Long eventId, String matchDate) {
        IPage<Schedule> schedulePage = pageSchedules(current, size, eventId, matchDate);

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
            }
        }

        return vo;
    }

    @Override
    public IPage<Schedule> pageSchedules(Integer current, Integer size, Long eventId, String matchDate) {
        Page<Schedule> page = new Page<>(current, size);

        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(eventId != null, Schedule::getEventId, eventId)
                .eq(StringUtils.hasText(matchDate), Schedule::getMatchDate, matchDate != null ? LocalDate.parse(matchDate) : null)
                .orderByAsc(Schedule::getMatchDate)
                .orderByAsc(Schedule::getStartTime);

        return this.page(page, wrapper);
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
