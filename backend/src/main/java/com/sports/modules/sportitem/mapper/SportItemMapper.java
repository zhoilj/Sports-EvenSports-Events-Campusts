package com.sports.modules.sportitem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sports.modules.sportitem.entity.SportItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 运动项目 Mapper
 *
 * @author sports
 */
@Mapper
public interface SportItemMapper extends BaseMapper<SportItem> {
}