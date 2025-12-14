package com.sports.modules.event.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sports.modules.event.dto.EventDTO;
import com.sports.modules.event.dto.EventQueryDTO;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.vo.EventVO;

import java.util.List;

/**
 * 赛事 Service
 *
 * @author sports
 */
public interface EventService extends IService<Event> {

    /**
     * 分页查询赛事
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<EventVO> pageEvents(EventQueryDTO queryDTO);

    /**
     * 获取赛事详情
     *
     * @param id 赛事ID
     * @return 赛事详情
     */
    EventVO getEventDetail(Long id);

    /**
     * 创建赛事
     *
     * @param eventDTO 赛事信息
     * @return 赛事ID
     */
    Long createEvent(EventDTO eventDTO);

    /**
     * 更新赛事
     *
     * @param id       赛事ID
     * @param eventDTO 赛事信息
     */
    void updateEvent(Long id, EventDTO eventDTO);

    /**
     * 删除赛事
     *
     * @param id 赛事ID
     */
    void deleteEvent(Long id);

    /**
     * 发布赛事
     *
     * @param id 赛事ID
     */
    void publishEvent(Long id);

    /**
     * 取消赛事
     *
     * @param id 赛事ID
     */
    void cancelEvent(Long id);

    /**
     * 获取可报名的赛事列表
     *
     * @return 赛事列表
     */
    List<EventVO> getAvailableEvents();

    /**
     * 获取进行中的赛事列表
     *
     * @return 赛事列表
     */
    List<EventVO> getOngoingEvents();
}