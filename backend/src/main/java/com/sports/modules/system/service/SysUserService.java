package com.sports.modules.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sports.modules.system.dto.UserDTO;
import com.sports.modules.system.dto.UserQueryDTO;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.vo.UserVO;

/**
 * 系统用户 Service
 *
 * @author sports
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    SysUser getByUsername(String username);

    /**
     * 分页查询用户
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<UserVO> pageUsers(UserQueryDTO queryDTO);

    /**
     * 创建用户
     *
     * @param userDTO 用户信息
     * @return 用户ID
     */
    Long createUser(UserDTO userDTO);

    /**
     * 更新用户
     *
     * @param id      用户ID
     * @param userDTO 用户信息
     */
    void updateUser(Long id, UserDTO userDTO);

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    void deleteUser(Long id);

    /**
     * 修改密码
     *
     * @param userId      用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 重置密码
     *
     * @param userId 用户ID
     */
    void resetPassword(Long userId);

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param status 状态
     */
    void updateStatus(Long userId, Integer status);

    /**
     * 获取用户详情
     *
     * @param id 用户ID
     * @return 用户信息
     */
    UserVO getUserById(Long id);

    /**
     * 切换用户状态
     *
     * @param userId 用户ID
     */
    void toggleStatus(Long userId);
}