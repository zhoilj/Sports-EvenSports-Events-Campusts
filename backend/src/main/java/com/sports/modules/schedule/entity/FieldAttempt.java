package com.sports.modules.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.sports.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("field_attempt")
public class FieldAttempt extends BaseEntity {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("schedule_entry_id")
    private Long scheduleEntryId;
    @TableField("attempt_no")
    private Integer attemptNo;
    private BigDecimal score;
    @TableField("is_foul")
    private Integer isFoul;
    @TableField("is_valid")
    private Integer isValid;
    @TableField("recorder_id")
    private Long recorderId;
    @TableField("recorded_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordedAt;
    private String remark;
}
