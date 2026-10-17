package com.sports.modules.registration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sports.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Relay roster member and order snapshot. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("registration_member")
public class RegistrationMember extends BaseEntity {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO) private Long id;
    @TableField("registration_id") private Long registrationId;
    @TableField("user_id") private Long userId;
    @TableField("member_order") private Integer memberOrder;
    @TableField("college_id") private Long collegeId;
    @TableField("class_id") private Long classId;
    @TableField("college_snapshot") private String collegeSnapshot;
    @TableField("class_snapshot") private String classSnapshot;
}
