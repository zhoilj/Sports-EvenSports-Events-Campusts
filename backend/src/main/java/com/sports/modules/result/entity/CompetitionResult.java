package com.sports.modules.result.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sports.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 比赛成绩实体
 *
 * @author sports
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("result")
public class CompetitionResult extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 成绩ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 赛事ID
     */
    private Long eventId;

    /**
     * 运动项目ID
     */
    private Long sportItemId;

    /**
     * 赛程ID
     */
    private Long scheduleId;

    /**
     * 报名记录ID（成绩必须关联已审核通过的报名）。
     */
    @TableField("registration_id")
    private Long registrationId;

    /**
     * Relay team ID; individual results use registrationId instead.
     */
    @TableField("relay_team_id")
    private Long relayTeamId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 成绩值
     */
    private BigDecimal score;

    /**
     * 成绩文本（数据库字段 score_text，兼容如 10.25秒、5.60米）。
     */
    @TableField("score_text")
    private String scoreText;

    /**
     * VALID / DNS / DNF / DQ / WD.
     */
    @TableField("result_status")
    private String resultStatus;

    /**
     * 排名
     */
    private Integer ranking;

    /**
     * 积分
     */
    private Integer points;

    /** 1-pending review, 2-reviewed, 3-returned. */
    @TableField("review_status")
    private Integer reviewStatus;

    @TableField("review_by")
    private Long reviewerId;

    @TableField("review_time")
    private LocalDateTime reviewTime;

    @TableField("review_remark")
    private String reviewRemark;

    @TableField("is_published")
    private Integer isPublished;

    @TableField("published_by")
    private Long publishedBy;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    /**
     * 录入人ID
     */
    @TableField("record_by")
    private Long recorderId;

    /**
     * 成绩录入时间
     */
    @TableField("record_time")
    private LocalDateTime recordTime;

    /**
     * 备注
     */
    private String remark;
}