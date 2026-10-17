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
        if (dto == null || dto.getEventId() == null || dto.getSportItemId() == null) {
            throw new BusinessException("赛事和项目不能为空");
        }

        Event event = eventService.getById(dto.getEventId());
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }
        if (event.getStatus() == null || event.getStatus() != 2) {
            throw new BusinessException(ResultCode.EVENT_NOT_PUBLISHED);
        }

        LocalDate today = LocalDate.now();
        if (event.getRegStartDate() != null && today.isBefore(event.getRegStartDate())) {
            throw new BusinessException(ResultCode.EVENT_REGISTRATION_NOT_STARTED);
        }
        if (event.getRegEndDate() != null && today.isAfter(event.getRegEndDate())) {
            throw new BusinessException(ResultCode.EVENT_REGISTRATION_ENDED);
        }

        // Lock the item row before checking capacity so concurrent submissions cannot oversubscribe it.
        SportItem sportItem = sportItemService.getOne(new LambdaQueryWrapper<SportItem>()
                .eq(SportItem::getId, dto.getSportItemId())
                .last("FOR UPDATE"));
        if (sportItem == null || !java.util.Objects.equals(sportItem.getEventId(), dto.getEventId())) {
            throw new BusinessException(ResultCode.SPORT_ITEM_NOT_FOUND);
        }
        if (sportItem.getStatus() == null || sportItem.getStatus() != 1) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_ALLOWED, "该项目当前未开放报名");
        }
        if (sportItem.getIsRelay() != null && sportItem.getIsRelay() == 1) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_ALLOWED, "该项目为接力项目，请通过接力组队报名");
        }

        SysUser user = sysUserService.getById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1
                || !"STUDENT".equals(user.getRole())) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_ALLOWED, "仅状态正常的学生账号可以报名");
        }

        Integer genderLimit = sportItem.getGenderLimit();
        if (genderLimit != null && genderLimit != 0 && !genderLimit.equals(user.getGender())) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_ALLOWED,
                    "该项目限制" + (genderLimit == 1 ? "男" : "女") + "性参加");
        }

        LambdaQueryWrapper<Registration> sameEntryQuery = new LambdaQueryWrapper<Registration>()
                .eq(Registration::getEventId, dto.getEventId())
                .eq(Registration::getSportItemId, dto.getSportItemId())
                .eq(Registration::getUserId, userId)
                .last("FOR UPDATE");
        Registration previous = this.getOne(sameEntryQuery, false);
        if (previous != null && (previous.getStatus() == 1 || previous.getStatus() == 2)) {
            throw new BusinessException(ResultCode.REGISTRATION_ALREADY_EXISTS);
        }

        long currentForItem = this.count(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getEventId, dto.getEventId())
                .eq(Registration::getSportItemId, dto.getSportItemId())
                .in(Registration::getStatus, 1, 2));
        Integer itemLimit = sportItem.getMaxParticipants();
        if (itemLimit != null && itemLimit > 0 && currentForItem >= itemLimit) {
            throw new BusinessException(ResultCode.REGISTRATION_LIMIT_EXCEEDED);
        }

        long currentForStudent = this.count(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getEventId, dto.getEventId())
                .eq(Registration::getUserId, userId)
                .in(Registration::getStatus, 1, 2));
        Integer perStudentLimit = event.getMaxItemsPerUser();
        if (perStudentLimit != null && perStudentLimit > 0 && currentForStudent >= perStudentLimit) {
            throw new BusinessException("已达到本赛事每人限报项目数（" + perStudentLimit + "项）");
        }

        if (previous != null) {
            // Reuse the existing row to satisfy the legacy unique key (user_id, sport_item_id, is_deleted).
            previous.setStatus(1);
            previous.setReviewerId(null);
            previous.setReviewTime(null);
            previous.setReviewRemark(null);
            previous.setRemark(dto.getRemark());
            this.updateById(previous);
            updateParticipantCount(sportItem);
            return previous.getId();
        }

        Registration registration = new Registration();
        registration.setEventId(dto.getEventId());
        registration.setSportItemId(dto.getSportItemId());
        registration.setUserId(userId);
        registration.setStatus(1);
        registration.setRemark(dto.getRemark());
        this.save(registration);
        updateParticipantCount(sportItem);
        return registration.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelRegistration(Long id) {
        Registration registration = this.getById(id);
        if (registration == null) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_FOUND);
        }

        Long currentUserId = SecurityUtils.getUserId();
        if (!registration.getUserId().equals(currentUserId) && !SecurityUtils.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (registration.getStatus() == null || registration.getStatus() != 1) {
            throw new BusinessException(ResultCode.REGISTRATION_CANNOT_CANCEL);
        }

        registration.setStatus(4);
        this.updateById(registration);
        SportItem item = sportItemService.getById(registration.getSportItemId());
        if (item != null) {
            updateParticipantCount(item);
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
        SportItem sportItem = sportItemService.getById(registration.getSportItemId());
        if (sportItem != null) {
            updateParticipantCount(sportItem);
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
                .in(Registration::getStatus, 1, 2);

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

    /**
     * Recalculate rather than increment/decrement a counter so retries and status transitions do not drift.
     * The caller runs inside a transaction and holds the sport-item row lock when creating registrations.
     */
    private void updateParticipantCount(SportItem item) {
        long activeCount = this.count(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getSportItemId, item.getId())
                .in(Registration::getStatus, 1, 2));
        item.setCurrentParticipants(Math.toIntExact(activeCount));
        sportItemService.updateById(item);
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