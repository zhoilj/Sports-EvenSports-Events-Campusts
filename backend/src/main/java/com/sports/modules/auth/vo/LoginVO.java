package com.sports.modules.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 VO
 *
 * @author sports
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    /**
     * 访问令牌
     */
    private String token;

    /**
     * 令牌类型
     */
    private String tokenType;

    /**
     * 过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 用户ID
     */
    private Long userId;

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
     * 头像
     */
    private String avatar;
}