package com.sports.modules.news.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sports.modules.news.entity.News;
import org.apache.ibatis.annotations.Mapper;

/**
 * 新闻/公告 Mapper
 *
 * @author sports
 */
@Mapper
public interface NewsMapper extends BaseMapper<News> {
}