package com.sports.modules.registration.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sports.modules.registration.dto.RegistrationDTO;
import com.sports.modules.registration.entity.Registration;
import com.sports.modules.registration.vo.RegistrationVO;

import java.util.List;

/**
 * 报名 Service
 *
 * @author sports
 */
public interface RegistrationService extends IService<Registration> {

    /**
     * 学生报名
     */
    Long register(RegistrationDTO dto);

    /**
     * 取消报名
     */
    void cancelRegistration(Long id);

    /**
     * 审核报名
     */
    void reviewRegistration(Long id, String status, String remark);

    /**
     * 分页查询报名列表
     */
    IPage<RegistrationVO> pageRegistrations(Integer current, Integer size, Long eventId, Long sportItemId, String status, String college, String className);

    /**
     * 分页获取当前学生的报名列表，支持按状态筛选。
     */
    IPage<RegistrationVO> pageMyRegistrations(Integer current, Integer size, String status);

    /**
     * 获取我的报名列表（兼容旧调用）。
     */
    List<RegistrationVO> getMyRegistrations();

    /**
     * 检查是否已报名
     */
    boolean hasRegistered(Long eventId, Long sportItemId, Long userId);
}