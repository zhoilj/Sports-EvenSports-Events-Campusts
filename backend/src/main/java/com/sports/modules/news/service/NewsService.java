package com.sports.modules.news.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sports.modules.news.entity.News;
import com.sports.modules.news.vo.NewsVO;

/**
 * 新闻/公告 Service
 *
 * @author sports
 */
public interface NewsService extends IService<News> {

    /**
     * 分页查询新闻（带VO转换）
     */
    IPage<NewsVO> pageNewsVO(Integer current, Integer size, String title, String type, Integer status);

    /**
     * 分页查询新闻
     */
    IPage<News> pageNews(Integer current, Integer size, String title, String type, Integer status);

    /**
     * 发布新闻
     */
    void publish(Long id);

    /**
     * 下架新闻
     */
    void offline(Long id);

    /**
     * 增加浏览次数
     */
    void incrementViewCount(Long id);
}
