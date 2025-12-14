package com.sports.modules.news.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.sports.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 新闻/公告实体
 *
 * @author sports
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("news")
public class News extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 新闻ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 公告类型
     */
    @TableField("news_type")
    private String type;

    /**
     * 内容
     */
    private String content;

    /**
     * 封面图片URL
     */
    @TableField("cover_image")
    private String coverImage;

    /**
     * 关联赛事ID（可选）
     */
    @TableField("event_id")
    private Long eventId;

    /**
     * 是否置顶（0-否 1-是）
     */
    @TableField("is_top")
    private Integer isTop;

    /**
     * 浏览次数
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 发布时间
     */
    @TableField("publish_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    /**
     * 状态（0-草稿 1-已发布）
     */
    private Integer status;

    /**
     * 创建人ID
     */
    @TableField("create_by")
    private Long createBy;
}
