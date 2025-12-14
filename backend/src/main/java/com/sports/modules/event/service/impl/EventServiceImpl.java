package com.sports.modules.event.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.ResultCode;
import com.sports.modules.event.dto.EventDTO;
import com.sports.modules.event.dto.EventQueryDTO;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.mapper.EventMapper;
import com.sports.modules.event.service.EventService;
import com.sports.modules.event.vo.EventVO;
import com.sports.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 赛事 Service 实现
 * 状态说明: 1-草稿 2-报名中 3-进行中 4-已结束
 *
 * @author sports
 */
@Service
@RequiredArgsConstructor
public class EventServiceImpl extends ServiceImpl<EventMapper, Event> implements EventService {

    // 状态常量
    private static final Integer STATUS_DRAFT = 1;
    private static final Integer STATUS_REGISTRATION = 2;
    private static final Integer STATUS_ONGOING = 3;
    private static final Integer STATUS_ENDED = 4;

    @Override
    public IPage<EventVO> pageEvents(EventQueryDTO queryDTO) {
        Page<Event> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        LambdaQueryWrapper<Event> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getName()), Event::getName, queryDTO.getName())
                .eq(StringUtils.hasText(queryDTO.getType()), Event::getType, queryDTO.getType())
                .eq(queryDTO.getStatus() != null, Event::getStatus, queryDTO.getStatus())
                .apply(queryDTO.getYear() != null, "YEAR(start_date) = {0}", queryDTO.getYear())
                .orderByDesc(Event::getCreateTime);

        IPage<Event> eventPage = this.page(page, wrapper);

        return eventPage.convert(this::convertToVO);
    }

    @Override
    public EventVO getEventDetail(Long id) {
        Event event = this.getById(id);
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }
        return convertToVO(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEvent(EventDTO eventDTO) {
        // 验证日期
        validateEventDates(eventDTO);

        Event event = new Event();
        BeanUtils.copyProperties(eventDTO, event);

        // 设置默认状态为草稿 (1-草稿)
        if (event.getStatus() == null) {
            event.setStatus(STATUS_DRAFT);
        }

        // 设置创建人
        event.setCreatorId(SecurityUtils.getUserId());

        this.save(event);
        return event.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEvent(Long id, EventDTO eventDTO) {
        Event event = this.getById(id);
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }

        // 已发布或进行中的赛事不能修改基本信息 (3-进行中)
        if (STATUS_ONGOING.equals(event.getStatus())) {
            throw new BusinessException("进行中的赛事不能修改");
        }

        // 验证日期
        validateEventDates(eventDTO);

        BeanUtils.copyProperties(eventDTO, event, "status", "creatorId");
        this.updateById(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEvent(Long id) {
        Event event = this.getById(id);
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }

        // 只能删除草稿状态的赛事 (1-草稿)
        if (!STATUS_DRAFT.equals(event.getStatus())) {
            throw new BusinessException("只能删除草稿状态的赛事");
        }

        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishEvent(Long id) {
        Event event = this.getById(id);
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }

        // 只有草稿状态的赛事才能发布 (1-草稿)
        if (!STATUS_DRAFT.equals(event.getStatus())) {
            throw new BusinessException("只有草稿状态的赛事才能发布");
        }

        // 发布后状态变为报名中 (2-报名中)
        event.setStatus(STATUS_REGISTRATION);
        this.updateById(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelEvent(Long id) {
        Event event = this.getById(id);
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }

        // 已结束的赛事不能再取消 (4-已结束)
        if (STATUS_ENDED.equals(event.getStatus())) {
            throw new BusinessException("已结束的赛事不能再取消");
        }

        // 取消后回到草稿状态 (1-草稿)
        event.setStatus(STATUS_DRAFT);
        this.updateById(event);
    }

    @Override
    public List<EventVO> getAvailableEvents() {
        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<Event> wrapper = new LambdaQueryWrapper<>();
        // 报名中的赛事 (2-报名中)
        wrapper.eq(Event::getStatus, STATUS_REGISTRATION)
                .le(Event::getRegStartDate, today)
                .ge(Event::getRegEndDate, today)
                .orderByAsc(Event::getRegEndDate);

        return this.list(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventVO> getOngoingEvents() {
        LambdaQueryWrapper<Event> wrapper = new LambdaQueryWrapper<>();
        // 进行中的赛事 (3-进行中)
        wrapper.eq(Event::getStatus, STATUS_ONGOING)
                .orderByAsc(Event::getStartDate);

        return this.list(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 验证赛事日期
     */
    private void validateEventDates(EventDTO eventDTO) {
        if (eventDTO.getStartDate().isAfter(eventDTO.getEndDate())) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        if (eventDTO.getRegStartDate().isAfter(eventDTO.getRegEndDate())) {
            throw new BusinessException("报名开始日期不能晚于报名截止日期");
        }
        if (eventDTO.getRegEndDate().isAfter(eventDTO.getStartDate())) {
            throw new BusinessException("报名截止日期不能晚于赛事开始日期");
        }
    }

    /**
     * 转换为 VO
     */
    private EventVO convertToVO(Event event) {
        EventVO vo = new EventVO();
        BeanUtils.copyProperties(event, vo);

        // 设置类型名称
        vo.setTypeName(getTypeName(event.getType()));

        // 设置状态名称
        vo.setStatusName(getStatusName(event.getStatus()));

        return vo;
    }

    private String getTypeName(String type) {
        if (type == null) return "";
        return switch (type) {
            case "TRACK_FIELD" -> "田径";
            case "BALL" -> "球类";
            case "SWIMMING" -> "游泳";
            case "OTHER" -> "其他";
            default -> type;
        };
    }

    private String getStatusName(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 1 -> "草稿";
            case 2 -> "报名中";
            case 3 -> "进行中";
            case 4 -> "已结束";
            default -> String.valueOf(status);
        };
    }
}
