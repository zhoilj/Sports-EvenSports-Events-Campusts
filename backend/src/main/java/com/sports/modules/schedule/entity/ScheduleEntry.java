package com.sports.modules.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sports.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("schedule_entry")
public class ScheduleEntry extends BaseEntity {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("schedule_id")
    private Long scheduleId;
    @TableField("registration_id")
    private Long registrationId;
    @TableField("relay_team_id")
    private Long relayTeamId;
    @TableField("group_no")
    private String groupNo;
    @TableField("lane_no")
    private Integer laneNo;
    @TableField("start_order")
    private Integer startOrder;
    @TableField("seed_score")
    private BigDecimal seedScore;
    @TableField("qualification_status")
    private String qualificationStatus;
}
