package com.sports.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sports.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户实体
 *
 * @author sports
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    @JsonIgnore
    private String password;

    /**
     * 真实姓名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 角色（ADMIN-管理员 TEACHER-教师 STUDENT-学生）
     */
    private String role;

    /**
     * 学号/工号
     */
    @TableField("student_no")
    private String studentNo;

    /**
     * 学院
     */
    @TableField("department")
    private String college;

    /**
     * 班级
     */
    @TableField("class_name")
    private String className;

    /** A=普通本科组，B=体育专业组；由管理员按学籍资料维护。 */
    @TableField("competition_group")
    private String competitionGroup;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 性别（0-未知 1-男 2-女）
     */
    private Integer gender;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 状态（0-禁用 1-正常）
     */
    private Integer status;
}