package com.sports.security.util;

import com.sports.common.exception.BusinessException;
import com.sports.common.result.ResultCode;
import com.sports.security.domain.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Security 工具类
 *
 * @author sports
 */
public class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户
     */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser) {
            return (LoginUser) principal;
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        return getLoginUser().getUsername();
    }

    /**
     * 获取当前用户角色
     */
    public static String getRole() {
        return getLoginUser().getRole();
    }

    /**
     * 判断是否为管理员
     */
    public static boolean isAdmin() {
        return "ADMIN".equals(getRole());
    }

    /**
     * 判断是否为教师
     */
    public static boolean isTeacher() {
        return "TEACHER".equals(getRole());
    }

    /**
     * 判断是否为学生
     */
    public static boolean isStudent() {
        return "STUDENT".equals(getRole());
    }

    /**
     * 判断是否已登录
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof LoginUser;
    }
}