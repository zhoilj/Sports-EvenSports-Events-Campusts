package com.sports.modules.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.sports.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 赛程实体
 *
 * @author sports
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("schedule")
public class Schedule extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 赛程ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 赛事ID
     */
    @TableField("event_id")
    private Long eventId;

    /**
     * 运动项目ID
     */
    @TableField("sport_item_id")
    private Long sportItemId;

    /**
     * 赛程名称（如: 预赛/决赛）
     */
    @TableField("schedule_name")
    private String scheduleName;

    /**
     * 轮次
     */
    @TableField("round_no")
    private Integer roundNo;

    /**
     * PRELIMINARY / SEMIFINAL / FINAL / FIELD_FINAL
     */
    @TableField("round_type")
    private String roundType;

    /**
     * 组别（如: A组/B组）
     */
    @TableField("group_no")
    private String groupNo;

    /**
     * 比赛日期
     */
    @TableField("match_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate matchDate;

    /**
     * 开始时间
     */
    @TableField("start_time")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    /**
     * 比赛场地
     */
    private String location;

    /**
     * 裁判
     */
    private String referee;

    /**
     * 状态（1-未开始 2-进行中 3-已结束）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
