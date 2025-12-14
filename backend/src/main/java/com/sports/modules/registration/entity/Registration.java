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
