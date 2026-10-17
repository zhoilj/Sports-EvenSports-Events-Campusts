package com.sports.modules.result.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sports.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scoring_rule")
public class ScoringRule extends BaseEntity {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("event_id")
    private Long eventId;
    @TableField("place_no")
    private Integer placeNo;
    private Integer points;
    @TableField("rule_name")
    private String ruleName;
}
