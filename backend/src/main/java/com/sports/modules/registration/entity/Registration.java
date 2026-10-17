package com.sports.modules.registration.entity;

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
 * 报名实体
 *
 * @author sports
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("registration")
public class Registration extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 报名ID
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
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 报名类型（INDIVIDUAL-个人 RELAY-接力）；旧记录默认为个人报名
     */
    @TableField("registration_type")
    private String registrationType = "INDIVIDUAL";

    /**
     * 接力队伍名称
     */
    @TableField("team_name")
    private String teamName;

    /**
     * 报名时的学院名称快照
     */
    @TableField("college_snapshot")
    private String collegeSnapshot;

    /**
     * 报名时的班级名称快照
     */
    @TableField("class_snapshot")
    private String classSnapshot;

    /**
     * 状态（1-待审核 2-已通过 3-已拒绝 4-已取消）
     */
    private Integer status;

    /**
     * 审核人ID
     */
    @TableField("audit_by")
    private Long reviewerId;

    /**
     * 审核时间
     */
    @TableField("audit_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;

    /**
     * 拒绝原因
     */
    @TableField("reject_reason")
    private String reviewRemark;

    /**
     * 报名备注
     */
    private String remark;
}
