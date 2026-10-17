package com.sports.modules.result.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("result_audit_log")
public class ResultAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("result_id")
    private Long resultId;
    private String action;
    @TableField("before_value")
    private String beforeValue;
    @TableField("after_value")
    private String afterValue;
    @TableField("operator_id")
    private Long operatorId;
    @TableField("operated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operatedAt;
    private String remark;
}
