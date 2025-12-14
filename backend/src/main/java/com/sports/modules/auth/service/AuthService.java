package com.sports.modules.auth.service;

import com.sports.modules.auth.dto.LoginDTO;
import com.sports.modules.auth.dto.RegisterDTO;
import com.sports.modules.auth.vo.LoginVO;

/**
 * 认证 Service
 *
 * @author sports
 */
public interface AuthService {

    /**
     * 登录
     *
     * @param loginDTO 登录信息
     * @return 登录结果
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 注册
     *
     * @param registerDTO 注册信息
     * @return 用户ID
     */
    Long register(RegisterDTO registerDTO);

    /**
     * 获取当前用户信息
     *
     * @return 用户信息
     */
    LoginVO getCurrentUser();
}