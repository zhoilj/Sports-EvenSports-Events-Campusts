package com.sports.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.ResultCode;
import com.sports.modules.system.dto.UserDTO;
import com.sports.modules.system.dto.UserQueryDTO;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.mapper.SysUserMapper;
import com.sports.modules.system.service.SysUserService;
import com.sports.modules.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 系统用户 Service 实现
 *
 * @author sports
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final PasswordEncoder passwordEncoder;

    /**
     * 默认密码
     */
    private static final String DEFAULT_PASSWORD = "123456";

    @Override
    public SysUser getByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }

    @Override
    public IPage<UserVO> pageUsers(UserQueryDTO queryDTO) {
        Page<SysUser> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getUsername()), SysUser::getUsername, queryDTO.getUsername())
                .like(StringUtils.hasText(queryDTO.getRealName()), SysUser::getRealName, queryDTO.getRealName())
                .eq(StringUtils.hasText(queryDTO.getRole()), SysUser::getRole, queryDTO.getRole())
                .like(StringUtils.hasText(queryDTO.getStudentNo()), SysUser::getStudentNo, queryDTO.getStudentNo())
                .like(StringUtils.hasText(queryDTO.getCollege()), SysUser::getCollege, queryDTO.getCollege())
                .eq(queryDTO.getStatus() != null, SysUser::getStatus, queryDTO.getStatus())
                .orderByDesc(SysUser::getCreateTime);

        IPage<SysUser> userPage = this.page(page, wrapper);

        return userPage.convert(this::convertToVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserDTO userDTO) {
        // 检查用户名是否已存在
        if (getByUsername(userDTO.getUsername()) != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        SysUser user = new SysUser();
        BeanUtils.copyProperties(userDTO, user);

        // 设置默认密码
        String password = StringUtils.hasText(userDTO.getPassword()) ? userDTO.getPassword() : DEFAULT_PASSWORD;
        user.setPassword(passwordEncoder.encode(password));

        // 设置默认状态
        if (user.getStatus() == null) {
            user.setStatus(1);
        }

        this.save(user);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long id, UserDTO userDTO) {
        SysUser user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 检查用户名是否被其他用户使用
        if (StringUtils.hasText(userDTO.getUsername()) && !userDTO.getUsername().equals(user.getUsername())) {
            SysUser existUser = getByUsername(userDTO.getUsername());
            if (existUser != null) {
                throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
            }
        }

        BeanUtils.copyProperties(userDTO, user, "password");

        // 如果传了密码则更新密码
        if (StringUtils.hasText(userDTO.getPassword())) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        SysUser user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, Integer status) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        user.setStatus(status);
        this.updateById(user);
    }

    /**
     * 转换为 VO
     */
    private UserVO convertToVO(SysUser user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);

        // 设置角色名称
        vo.setRoleName(getRoleName(user.getRole()));

        // 设置性别名称
        vo.setGenderName(getGenderName(user.getGender()));

        // 设置状态名称
        vo.setStatusName(user.getStatus() == 1 ? "正常" : "禁用");

        return vo;
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

    private String getGenderName(Integer gender) {
        if (gender == null) return "";
        return switch (gender) {
            case 1 -> "男";
            case 2 -> "女";
            default -> "未知";
        };
    }

    @Override
    public UserVO getUserById(Long id) {
        SysUser user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return convertToVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        // 切换状态: 1->0, 0->1
        user.setStatus(user.getStatus() == 1 ? 0 : 1);
        this.updateById(user);
    }
}