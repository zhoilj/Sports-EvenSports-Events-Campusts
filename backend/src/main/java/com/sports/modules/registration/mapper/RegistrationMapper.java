package com.sports.modules.registration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sports.modules.registration.entity.Registration;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报名 Mapper
 *
 * @author sports
 */
@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {
}