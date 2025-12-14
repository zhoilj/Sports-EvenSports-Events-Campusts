package com.sports.modules.news.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.ResultCode;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.service.EventService;
import com.sports.modules.news.entity.News;
import com.sports.modules.news.mapper.NewsMapper;
import com.sports.modules.news.service.NewsService;
import com.sports.modules.news.vo.NewsVO;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 新闻/公告 Service 实现
 *
 * @author sports
 */
@Service
@RequiredArgsConstructor
public class NewsServiceImpl extends ServiceImpl<NewsMapper, News> implements NewsService {

    private final EventService eventService;
    private final SysUserService sysUserService;

    // 状态常量: 0-草稿 1-已发布 2-已下架
    private static final Integer STATUS_DRAFT = 0;
    private static final Integer STATUS_PUBLISHED = 1;
    private static final Integer STATUS_OFFLINE = 2;

    // 类型名称映射
    private static final Map<String, String> TYPE_NAMES = Map.of(
            "ANNOUNCEMENT", "公告",
            "NEWS", "新闻",
            "NOTICE", "通知"
    );

    // 状态名称映射
    private static final Map<Integer, String> STATUS_NAMES = Map.of(
            0, "草稿",
            1, "已发布",
            2, "已下架"
    );

    @Override
    public IPage<NewsVO> pageNewsVO(Integer current, Integer size, String title, String type, Integer status) {
        IPage<News> newsPage = pageNews(current, size, title, type, status);
        return newsPage.convert(this::convertToVO);
    }

    private NewsVO convertToVO(News news) {
        NewsVO vo = new NewsVO();
        BeanUtils.copyProperties(news, vo);

        // 设置类型名称
        vo.setTypeName(TYPE_NAMES.getOrDefault(news.getType(), news.getType()));

        // 设置状态名称
        vo.setStatusName(STATUS_NAMES.getOrDefault(news.getStatus(), String.valueOf(news.getStatus())));

        // 获取赛事名称
        if (news.getEventId() != null) {
            Event event = eventService.getById(news.getEventId());
            if (event != null) {
                vo.setEventName(event.getName());
            }
        }

        // 获取作者名称
        if (news.getCreateBy() != null) {
            SysUser user = sysUserService.getById(news.getCreateBy());
            if (user != null) {
                vo.setAuthorName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            }
        }

        return vo;
    }

    @Override
    public IPage<News> pageNews(Integer current, Integer size, String title, String type, Integer status) {
        Page<News> page = new Page<>(current, size);

        LambdaQueryWrapper<News> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(title), News::getTitle, title)
                .eq(StringUtils.hasText(type), News::getType, type)
                .eq(status != null, News::getStatus, status)
                .orderByDesc(News::getIsTop)
                .orderByDesc(News::getPublishTime)
                .orderByDesc(News::getCreateTime);

        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        News news = this.getById(id);
        if (news == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        news.setStatus(STATUS_PUBLISHED);
        news.setPublishTime(LocalDateTime.now());
        this.updateById(news);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        News news = this.getById(id);
        if (news == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        news.setStatus(STATUS_OFFLINE);
        this.updateById(news);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementViewCount(Long id) {
        News news = this.getById(id);
        if (news != null) {
            news.setViewCount(news.getViewCount() == null ? 1 : news.getViewCount() + 1);
            this.updateById(news);
        }
    }
}
