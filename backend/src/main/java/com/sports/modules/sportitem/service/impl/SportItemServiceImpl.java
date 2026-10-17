package com.sports.modules.sportitem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.mapper.SportItemMapper;
import com.sports.modules.sportitem.service.SportItemService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 运动项目 Service 实现
 *
 * @author sports
 */
@Service
public class SportItemServiceImpl extends ServiceImpl<SportItemMapper, SportItem> implements SportItemService {

    @Override
    public List<SportItem> getByEventId(Long eventId) {
        LambdaQueryWrapper<SportItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SportItem::getEventId, eventId)
                .eq(SportItem::getStatus, 1)
                .orderByAsc(SportItem::getId);
        return this.list(wrapper);
    }

    @Override
    public IPage<SportItem> pageItems(Integer current, Integer size, Long eventId, String type, Integer status, String competitionGroup) {
        Page<SportItem> page = new Page<>(current, size);

        LambdaQueryWrapper<SportItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(eventId != null, SportItem::getEventId, eventId)
                .eq(StringUtils.hasText(type), SportItem::getType, type)
                .eq(status != null, SportItem::getStatus, status)
                .eq(StringUtils.hasText(competitionGroup), SportItem::getCompetitionGroup, competitionGroup)
                .orderByAsc(SportItem::getCompetitionGroup)
                .orderByAsc(SportItem::getType)
                .orderByAsc(SportItem::getName);

        return this.page(page, wrapper);
    }
}