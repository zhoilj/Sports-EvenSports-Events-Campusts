package com.sports.modules.sportitem.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sports.modules.sportitem.entity.SportItem;

import java.util.List;

/**
 * 运动项目 Service
 *
 * @author sports
 */
public interface SportItemService extends IService<SportItem> {

    /**
     * 根据赛事ID获取项目列表
     */
    List<SportItem> getByEventId(Long eventId);

    /**
     * 分页查询项目
     */
    IPage<SportItem> pageItems(Integer current, Integer size, Long eventId, String name, String type, Integer status, String competitionGroup);
}