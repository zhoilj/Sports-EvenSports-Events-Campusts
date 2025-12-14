package com.sports.modules.news.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.Result;
import com.sports.common.result.ResultCode;
import com.sports.modules.news.entity.News;
import com.sports.modules.news.service.NewsService;
import com.sports.modules.news.vo.NewsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 新闻/公告 Controller
 *
 * @author sports
 */
@Tag(name = "新闻公告管理", description = "新闻公告的增删改查")
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @Operation(summary = "分页查询新闻")
    @GetMapping
    public Result<IPage<NewsVO>> pageNews(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status) {
        return Result.success(newsService.pageNewsVO(current, size, title, type, status));
    }

    @Operation(summary = "获取新闻详情")
    @GetMapping("/{id}")
    public Result<News> getById(@PathVariable Long id) {
        News news = newsService.getById(id);
        if (news == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 增加浏览次数
        newsService.incrementViewCount(id);
        return Result.success(news);
    }

    @Operation(summary = "创建新闻")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> create(@Valid @RequestBody News news) {
        // 设置默认值
        if (news.getStatus() == null) {
            news.setStatus(0); // 草稿
        }
        if (news.getViewCount() == null) {
            news.setViewCount(0);
        }
        if (news.getIsTop() == null) {
            news.setIsTop(0);
        }
        newsService.save(news);
        return Result.success(news.getId());
    }

    @Operation(summary = "更新新闻")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody News news) {
        News existing = newsService.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        news.setId(id);
        newsService.updateById(news);
        return Result.success();
    }

    @Operation(summary = "删除新闻")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        News existing = newsService.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        newsService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "发布新闻")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> publish(@PathVariable Long id) {
        newsService.publish(id);
        return Result.success();
    }

    @Operation(summary = "下架新闻")
    @PostMapping("/{id}/offline")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> offline(@PathVariable Long id) {
        newsService.offline(id);
        return Result.success();
    }
}
