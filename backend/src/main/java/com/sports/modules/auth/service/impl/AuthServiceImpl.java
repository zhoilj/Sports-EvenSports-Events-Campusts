package com.sports.modules.auth.service.impl;

import com.sports.common.exception.BusinessException;
import com.sports.common.result.ResultCode;
import com.sports.modules.auth.dto.LoginDTO;
import com.sports.modules.auth.dto.RegisterDTO;
import com.sports.modules.auth.service.AuthService;
import com.sports.modules.auth.vo.LoginVO;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.service.SysUserService;
import com.sports.security.util.JwtUtils;
import com.sports.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证 Service 实现
 *
 * @author sports
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 查询用户
        SysUser user = sysUserService.getByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }

        // 验证密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }

        // 检查状态
        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // 生成 Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        return LoginVO.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiration / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .roleName(getRoleName(user.getRole()))
                .avatar(user.getAvatar())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterDTO registerDTO) {
        // 验证两次密码是否一致
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_NOT_MATCH);
        }

        // 检查用户名是否已存在
        if (sysUserService.getByUsername(registerDTO.getUsername()) != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRealName(registerDTO.getRealName());
        user.setRole("STUDENT"); // 注册默认为学生
        user.setStudentNo(registerDTO.getStudentNo());
        user.setCollege(registerDTO.getCollege());
        user.setClassName(registerDTO.getClassName());
        user.setPhone(registerDTO.getPhone());
        user.setEmail(registerDTO.getEmail());
        // 性别转换: M->1, F->2, 默认0
        String genderStr = registerDTO.getGender();
        Integer gender = "M".equals(genderStr) ? 1 : ("F".equals(genderStr) ? 2 : 0);
        user.setGender(gender);
        user.setStatus(1);

        sysUserService.save(user);
        return user.getId();
    }

    @Override
    public LoginVO getCurrentUser() {
        Long userId = SecurityUtils.getUserId();
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        return LoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .roleName(getRoleName(user.getRole()))
                .avatar(user.getAvatar())
                .build();
    }

    private String getRoleName(String role) {
        if (role == null) return "";
        return switch (role) {
            case "ADMIN" -> "管理员";
            case "TEACHER" -> "教师";
            case "STUDENT" -> "学生";
            default -> role;
        };
    }
}