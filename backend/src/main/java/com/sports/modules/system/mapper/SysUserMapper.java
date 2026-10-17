package com.sports.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sports.modules.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统用户 Mapper
 *
 * @author sports
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 锁定报名涉及的学生行，串行化同一运动员的并发报名校验。
     */
    @Select("SELECT id FROM sys_user WHERE id = #{userId} AND is_deleted = 0 FOR UPDATE")
    Long lockForRegistration(@Param("userId") Long userId);
}