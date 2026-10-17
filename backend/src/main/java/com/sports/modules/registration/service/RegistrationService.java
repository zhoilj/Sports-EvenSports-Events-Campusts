package com.sports.modules.registration.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sports.modules.registration.dto.RegistrationDTO;
import com.sports.modules.registration.entity.Registration;
import com.sports.modules.registration.vo.RegistrationVO;
import com.sports.modules.registration.vo.RegistrationMemberVO;

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
    IPage<RegistrationVO> pageRegistrations(Integer current, Integer size, Long eventId, Long sportItemId, String status);

    /**
     * 获取我的报名列表（分页，支持状态筛选；接力队员也能看到所属队伍）
     */
    IPage<RegistrationVO> getMyRegistrations(Integer current, Integer size, String status);

    /**
     * 查询同学院可选接力队员，仅返回必要的学生信息
     */
    List<RegistrationMemberVO> listEligibleAthletes(String keyword);

    /**
     * 检查是否已报名
     */
    boolean hasRegistered(Long eventId, Long sportItemId, Long userId);
}