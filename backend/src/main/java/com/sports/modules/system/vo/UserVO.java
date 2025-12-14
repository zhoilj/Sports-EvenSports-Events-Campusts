package com.sports.modules.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户 VO
 *
 * @author sports
 */
@Data
public class UserVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 角色
     */
    private String role;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 学号/工号
     */
    private String studentNo;

    /**
     * 学院
     */
    private String college;

    /**
     * 班级
     */
    private String className;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 性别
     */
    private String gender;

    /**
     * 性别名称
     */
    private String genderName;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}