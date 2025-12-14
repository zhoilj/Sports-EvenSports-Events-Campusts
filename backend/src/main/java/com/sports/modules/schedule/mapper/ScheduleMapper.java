package com.sports.modules.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sports.modules.schedule.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 赛程 Mapper
 *
 * @author sports
 */
@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {
}