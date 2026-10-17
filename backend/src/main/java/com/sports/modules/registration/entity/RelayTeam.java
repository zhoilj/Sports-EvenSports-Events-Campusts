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

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("relay_team")
public class RelayTeam extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("event_id")
    private Long eventId;

    @TableField("sport_item_id")
    private Long sportItemId;

    @TableField("team_name")
    private String teamName;

    private String college;

    @TableField("class_name")
    private String className;

    @TableField("creator_user_id")
    private Long creatorUserId;

    /** 1-pending, 2-approved, 3-rejected, 4-cancelled */
    private Integer status;

    @TableField("review_by")
    private Long reviewerId;

    @TableField("review_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;

    @TableField("review_remark")
    private String reviewRemark;
}
