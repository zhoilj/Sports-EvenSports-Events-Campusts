package com.sports.modules.registration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.ResultCode;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.service.EventService;
import com.sports.modules.registration.dto.RegistrationDTO;
import com.sports.modules.registration.entity.Registration;
import com.sports.modules.registration.mapper.RegistrationMapper;
import com.sports.modules.registration.service.RegistrationService;
import com.sports.modules.registration.vo.RegistrationVO;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.service.SportItemService;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.service.SysUserService;
import com.sports.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 报名 Service 实现
 *
 * @author sports
 */
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl extends ServiceImpl<RegistrationMapper, Registration> implements RegistrationService {

    private final EventService eventService;
    private final SportItemService sportItemService;
    private final SysUserService sysUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegistrationDTO dto) {
        Long userId = SecurityUtils.getUserId();

        // 检查是否已报名
        if (hasRegistered(dto.getEventId(), dto.getSportItemId(), userId)) {
            throw new BusinessException(ResultCode.REGISTRATION_ALREADY_EXISTS);
        }

        // 检查赛事状态
        Event event = eventService.getById(dto.getEventId());
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }
        // 检查赛事状态 (2-报名中)
        if (event.getStatus() == null || event.getStatus() != 2) {
            throw new BusinessException(ResultCode.EVENT_NOT_PUBLISHED);
        }

        // 检查报名时间
        LocalDate today = LocalDate.now();
        if (event.getRegStartDate() != null && today.isBefore(event.getRegStartDate())) {
            throw new BusinessException(ResultCode.EVENT_REGISTRATION_NOT_STARTED);
        }
        if (event.getRegEndDate() != null && today.isAfter(event.getRegEndDate())) {
            throw new BusinessException(ResultCode.EVENT_REGISTRATION_ENDED);
        }

        // 检查项目
        SportItem sportItem = sportItemService.getById(dto.getSportItemId());
        if (sportItem == null || !sportItem.getEventId().equals(dto.getEventId())) {
            throw new BusinessException(ResultCode.SPORT_ITEM_NOT_FOUND);
        }

        // 检查人数限制
        int currentParticipants = sportItem.getCurrentParticipants() == null ? 0 : sportItem.getCurrentParticipants();
        // max_participants 为 0 或 null 时表示不限制人数。
        if (sportItem.getMaxParticipants() != null && sportItem.getMaxParticipants() > 0
                && currentParticipants >= sportItem.getMaxParticipants()) {
            throw new BusinessException(ResultCode.REGISTRATION_LIMIT_EXCEEDED);
        }

        // 检查性别限制 (0-不限 1-仅男 2-仅女)
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_ALLOWED, "未找到当前学生信息");
        }
        Integer genderLimit = sportItem.getGenderLimit();
        if (genderLimit != null && genderLimit != 0) {
            // 用户性别需要与限制匹配
            if (!genderLimit.equals(user.getGender())) {
                throw new BusinessException(ResultCode.REGISTRATION_NOT_ALLOWED,
                    "该项目限制" + (genderLimit == 1 ? "男" : "女") + "性参加");
            }
        }

        // 创建报名记录
        Registration registration = new Registration();
        registration.setEventId(dto.getEventId());
        registration.setSportItemId(dto.getSportItemId());
        registration.setUserId(userId);
        registration.setStatus(1);  // 1-待审核
        registration.setRemark(dto.getRemark());

        this.save(registration);

        // 更新项目报名人数
        sportItem.setCurrentParticipants(currentParticipants + 1);
        sportItemService.updateById(sportItem);

        return registration.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelRegistration(Long id) {
        Registration registration = this.getById(id);
        if (registration == null) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_FOUND);
        }

        // 只能取消自己的报名
        if (!registration.getUserId().equals(SecurityUtils.getUserId()) && !SecurityUtils.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        // 只能取消待审核的报名 (1-待审核)
        if (registration.getStatus() == null || registration.getStatus() != 1) {
            throw new BusinessException(ResultCode.REGISTRATION_CANNOT_CANCEL);
        }

        registration.setStatus(4);  // 4-已取消
        this.updateById(registration);

        // 更新项目报名人数
        SportItem sportItem = sportItemService.getById(registration.getSportItemId());
        if (sportItem != null && sportItem.getCurrentParticipants() > 0) {
            sportItem.setCurrentParticipants(sportItem.getCurrentParticipants() - 1);
            sportItemService.updateById(sportItem);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewRegistration(Long id, String status, String remark) {
        Registration registration = this.getById(id);
        if (registration == null) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_FOUND);
        }

        // 只能审核待审核的报名 (1-待审核)
        if (registration.getStatus() == null || registration.getStatus() != 1) {
            throw new BusinessException("只能审核待审核的报名");
        }

        // 状态转换: APPROVED->2, REJECTED->3。拒绝无效状态，避免静默更新。
        Integer statusCode;
        if ("APPROVED".equalsIgnoreCase(status)) {
            statusCode = 2;
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            statusCode = 3;
        } else {
            throw new BusinessException("不支持的报名审核状态");
        }
        registration.setStatus(statusCode);
        registration.setReviewerId(SecurityUtils.getUserId());
        registration.setReviewTime(LocalDateTime.now());
        registration.setReviewRemark(remark);

        this.updateById(registration);

        // 如果拒绝(3)，减少报名人数
        if (statusCode == 3) {
            SportItem sportItem = sportItemService.getById(registration.getSportItemId());
            if (sportItem != null && sportItem.getCurrentParticipants() > 0) {
                sportItem.setCurrentParticipants(sportItem.getCurrentParticipants() - 1);
                sportItemService.updateById(sportItem);
            }
        }
    }

    @Override
    public IPage<RegistrationVO> pageRegistrations(Integer current, Integer size, Long eventId, Long sportItemId, String status) {
        Page<Registration> page = new Page<>(current, size);

        Integer statusCode = parseStatus(status);
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(eventId != null, Registration::getEventId, eventId)
                .eq(sportItemId != null, Registration::getSportItemId, sportItemId)
                .eq(statusCode != null, Registration::getStatus, statusCode)
                .orderByDesc(Registration::getCreateTime);

        IPage<Registration> registrationPage = this.page(page, wrapper);

        // 使用批量转换优化N+1查询
        return convertToVOPage(registrationPage);
    }

    @Override
    public IPage<RegistrationVO> pageMyRegistrations(Integer current, Integer size, String status) {
        Page<Registration> page = new Page<>(current, size);
        Integer statusCode = parseStatus(status);
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getUserId, SecurityUtils.getUserId())
                .eq(statusCode != null, Registration::getStatus, statusCode)
                .orderByDesc(Registration::getCreateTime);
        return convertToVOPage(this.page(page, wrapper));
    }

    @Override
    public List<RegistrationVO> getMyRegistrations() {
        Long userId = SecurityUtils.getUserId();

        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getUserId, userId)
                .orderByDesc(Registration::getCreateTime);

        List<Registration> registrations = this.list(wrapper);
        return convertToVOList(registrations);
    }

    @Override
    public boolean hasRegistered(Long eventId, Long sportItemId, Long userId) {
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getEventId, eventId)
                .eq(Registration::getSportItemId, sportItemId)
                .eq(Registration::getUserId, userId)
                .ne(Registration::getStatus, 4);  // 4-已取消

        return this.count(wrapper) > 0;
    }

    /**
     * 批量转换为VO分页（优化N+1查询）
     */
    private IPage<RegistrationVO> convertToVOPage(IPage<Registration> page) {
        List<Registration> records = page.getRecords();
        List<RegistrationVO> voList = convertToVOList(records);
        
        Page<RegistrationVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 批量转换为VO列表（优化N+1查询）
     */
    private List<RegistrationVO> convertToVOList(List<Registration> registrations) {
        if (registrations.isEmpty()) {
            return List.of();
        }

        // 收集所有需要查询的ID
        Set<Long> eventIds = registrations.stream().map(Registration::getEventId).collect(Collectors.toSet());
        Set<Long> sportItemIds = registrations.stream().map(Registration::getSportItemId).collect(Collectors.toSet());
        Set<Long> userIds = registrations.stream().map(Registration::getUserId).collect(Collectors.toSet());
        Set<Long> reviewerIds = registrations.stream()
                .map(Registration::getReviewerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        userIds.addAll(reviewerIds);

        // 批量查询关联数据
        Map<Long, Event> eventMap = eventService.listByIds(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, e -> e));
        Map<Long, SportItem> sportItemMap = sportItemService.listByIds(sportItemIds).stream()
                .collect(Collectors.toMap(SportItem::getId, s -> s));
        Map<Long, SysUser> userMap = sysUserService.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        // 转换
        return registrations.stream().map(registration -> {
            RegistrationVO vo = new RegistrationVO();
            BeanUtils.copyProperties(registration, vo);

            Event event = eventMap.get(registration.getEventId());
            if (event != null) {
                vo.setEventName(event.getName());
            }

            SportItem sportItem = sportItemMap.get(registration.getSportItemId());
            if (sportItem != null) {
                vo.setSportItemName(sportItem.getName());
            }

            SysUser user = userMap.get(registration.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setRealName(user.getRealName());
                vo.setStudentNo(user.getStudentNo());
                vo.setCollege(user.getCollege());
            }

            if (registration.getReviewerId() != null) {
                SysUser reviewer = userMap.get(registration.getReviewerId());
                if (reviewer != null) {
                    vo.setReviewerName(reviewer.getRealName());
                }
            }

            vo.setStatus(getStatusCode(registration.getStatus()));
            vo.setStatusName(getStatusName(registration.getStatus()));
            return vo;
        }).collect(Collectors.toList());
    }

    private Integer parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return switch (status.trim().toUpperCase()) {
            case "PENDING", "1" -> 1;
            case "APPROVED", "2" -> 2;
            case "REJECTED", "3" -> 3;
            case "CANCELLED", "CANCELED", "4" -> 4;
            default -> throw new BusinessException("不支持的报名状态：" + status);
        };
    }

    private String getStatusCode(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 1 -> "PENDING";
            case 2 -> "APPROVED";
            case 3 -> "REJECTED";
            case 4 -> "CANCELLED";
            default -> String.valueOf(status);
        };
    }

    private String getStatusName(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 1 -> "待审核";
            case 2 -> "已通过";
            case 3 -> "已拒绝";
            case 4 -> "已取消";
            default -> String.valueOf(status);
        };
    }
}