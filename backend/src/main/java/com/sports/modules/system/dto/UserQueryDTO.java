package com.sports.modules.system.dto;

import lombok.Data;

/**
 * 用户查询 DTO
 *
 * @author sports
 */
@Data
public class UserQueryDTO {

    /**
     * 当前页码
     */
    private Integer current = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;

    /**
     * 用户名（模糊查询）
     */
    private String username;

    /**
     * 真实姓名（模糊查询）
     */
    private String realName;

    /**
     * 角色
     */
    private String role;

    /**
     * 学号/工号
     */
    private String studentNo;

    /**
     * 学院
     */
    private String college;

    /**
     * 状态
     */
    private Integer status;
}