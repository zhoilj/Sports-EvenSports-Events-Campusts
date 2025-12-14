package com.sports.modules.result.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sports.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

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
     * 用户ID
     */
    private Long userId;

    /**
     * 成绩值
     */
    private BigDecimal score;

    /**
     * 成绩单位
     */
    private String scoreUnit;

    /**
     * 排名
     */
    private Integer ranking;

    /**
     * 积分
     */
    private Integer points;

    /**
     * 是否破纪录
     */
    private Boolean isRecord;

    /**
     * 录入人ID
     */
    private Long recorderId;

    /**
     * 备注
     */
    private String remark;
}