package com.sports.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sports.modules.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}