package com.sports.modules.news.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新闻/公告 VO
 *
 * @author sports
 */
@Data
public class NewsVO {

    /**
     * 新闻ID
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 类型编码
     */
    private String type;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 内容
     */
    private String content;

    /**
     * 封面图片URL
     */
    private String coverImage;

    /**
     * 关联赛事ID
     */
    private Long eventId;

    /**
     * 关联赛事名称
     */
    private String eventName;

    /**
     * 是否置顶
     */
    private Integer isTop;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 作者名称
     */
    private String authorName;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
