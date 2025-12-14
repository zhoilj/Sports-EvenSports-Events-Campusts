package com.sports.modules.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sports.modules.event.entity.Event;
import org.apache.ibatis.annotations.Mapper;

/**
 * 赛事 Mapper
 *
 * @author sports
 */
@Mapper
public interface EventMapper extends BaseMapper<Event> {
}