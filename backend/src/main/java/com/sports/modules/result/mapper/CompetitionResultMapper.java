package com.sports.modules.result.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sports.modules.result.entity.CompetitionResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 比赛成绩 Mapper
 *
 * @author sports
 */
@Mapper
public interface CompetitionResultMapper extends BaseMapper<CompetitionResult> {
}