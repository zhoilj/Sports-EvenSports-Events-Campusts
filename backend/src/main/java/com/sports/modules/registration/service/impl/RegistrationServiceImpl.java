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
import com.sports.modules.registration.entity.RegistrationMember;
import com.sports.modules.registration.entity.RegistrationStatus;
import com.sports.modules.registration.mapper.RegistrationMapper;
import com.sports.modules.registration.mapper.RegistrationMemberMapper;
import com.sports.modules.registration.service.RegistrationService;
import com.sports.modules.registration.vo.RegistrationMemberVO;
import com.sports.modules.registration.vo.RegistrationVO;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.mapper.SportItemMapper;
import com.sports.modules.sportitem.service.SportItemService;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.mapper.SysUserMapper;
import com.sports.modules.system.service.SysUserService;
import com.sports.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 报名业务实现。报名记录按个人/队伍计数，接力成员单独保存并参与资格校验。
 */
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl extends ServiceImpl<RegistrationMapper, Registration> implements RegistrationService {

    private static final int PENDING = 1;
    private static final int APPROVED = 2;
    private static final int REJECTED = 3;
    private static final int CANCELLED = 4;

    private final EventService eventService;
    private final SportItemService sportItemService;
    private final SportItemMapper sportItemMapper;
    private final SysUserService sysUserService;
    private final SysUserMapper sysUserMapper;
    private final RegistrationMemberMapper registrationMemberMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegistrationDTO dto) {
        Long applicantId = SecurityUtils.getUserId();

        Event event = eventService.getById(dto.getEventId());
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }
        if (!Integer.valueOf(2).equals(event.getStatus())) {
            throw new BusinessException(ResultCode.EVENT_NOT_PUBLISHED);
        }
        LocalDate today = LocalDate.now();
        if (event.getRegStartDate() == null || event.getRegEndDate() == null) {
            throw new BusinessException("赛事报名起止日期尚未配置");
        }
        if (today.isBefore(event.getRegStartDate())) {
            throw new BusinessException(ResultCode.EVENT_REGISTRATION_NOT_STARTED);
        }
        if (today.isAfter(event.getRegEndDate())) {
            throw new BusinessException(ResultCode.EVENT_REGISTRATION_ENDED);
        }

        SportItem item = sportItemService.getById(dto.getSportItemId());
        if (item == null || !dto.getEventId().equals(item.getEventId())) {
            throw new BusinessException(ResultCode.SPORT_ITEM_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(item.getStatus())) {
            throw new BusinessException(ResultCode.SPORT_ITEM_DISABLED);
        }

        String mode = normalizeMode(item.getParticipationMode());
        List<Long> memberIds = resolveMemberIds(dto, mode, applicantId, item);

        // 按用户 ID 固定顺序加锁，避免同一运动员的并发报名绕过限报校验。
        for (Long memberId : memberIds.stream().sorted().toList()) {
            if (sysUserMapper.lockForRegistration(memberId) == null) {
                throw new BusinessException("运动员账号不存在或已删除");
            }
        }

        Map<Long, SysUser> athleteMap = sysUserService.listByIds(memberIds).stream()
                .collect(Collectors.toMap(SysUser::getId, user -> user, (left, right) -> left));
        if (athleteMap.size() != memberIds.size()) {
            throw new BusinessException("部分运动员账号不存在或已删除");
        }

        List<SysUser> athletes = memberIds.stream().map(athleteMap::get).toList();
        for (SysUser athlete : athletes) {
            validateStudentProfile(athlete);
            validateGender(item, athlete);
        }
        validateSameCollege(athletes);

        validateEventEntryLimits(event, item, memberIds);

        // 原子预占项目名额；后续报名或队员写入失败时由事务回滚。
        if (sportItemMapper.reserveParticipantSlot(item.getId()) == 0) {
            throw new BusinessException(ResultCode.REGISTRATION_LIMIT_EXCEEDED);
        }

        SysUser applicant = athleteMap.get(applicantId);
        Registration registration = new Registration();
        registration.setEventId(event.getId());
        registration.setSportItemId(item.getId());
        registration.setUserId(applicantId);
        registration.setStatus(PENDING);
        registration.setRegistrationType(mode);
        registration.setTeamName("RELAY".equals(mode) ? dto.getTeamName().trim() : null);
        registration.setCollegeSnapshot(applicant.getCollege());
        registration.setClassSnapshot(applicant.getClassName());
        registration.setRemark(dto.getRemark());
        if (!this.save(registration) || registration.getId() == null) {
            throw new BusinessException("报名记录保存失败");
        }

        if ("RELAY".equals(mode)) {
            for (int index = 0; index < memberIds.size(); index++) {
                SysUser athlete = athleteMap.get(memberIds.get(index));
                RegistrationMember member = new RegistrationMember();
                member.setRegistrationId(registration.getId());
                member.setUserId(athlete.getId());
                member.setMemberOrder(index + 1);
                member.setCollegeId(athlete.getCollegeId());
                member.setClassId(athlete.getClassId());
                member.setCollegeSnapshot(athlete.getCollege());
                member.setClassSnapshot(athlete.getClassName());
                if (registrationMemberMapper.insert(member) != 1) {
                    throw new BusinessException("接力队员名单保存失败");
                }
            }
        }
        return registration.getId();
    }

    private List<Long> resolveMemberIds(RegistrationDTO dto, String mode, Long applicantId, SportItem item) {
        if ("INDIVIDUAL".equals(mode)) {
            if (dto.getMemberUserIds() != null && !dto.getMemberUserIds().isEmpty()) {
                throw new BusinessException("个人项目不能提交接力队员名单");
            }
            return List.of(applicantId);
        }
        if (!StringUtils.hasText(dto.getTeamName())) {
            throw new BusinessException("接力报名必须填写队伍名称");
        }
        if (dto.getTeamName().trim().length() > 100) {
            throw new BusinessException("接力队伍名称最多100个字符");
        }
        if (dto.getMemberUserIds() == null || dto.getMemberUserIds().isEmpty()) {
            throw new BusinessException("接力报名必须提交完整队员名单");
        }

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(dto.getMemberUserIds());
        if (uniqueIds.contains(null) || uniqueIds.size() != dto.getMemberUserIds().size()) {
            throw new BusinessException("接力队员名单存在空值或重复队员");
        }
        if (!uniqueIds.contains(applicantId)) {
            throw new BusinessException("报名发起人必须列入接力队员名单");
        }
        Integer requiredCount = item.getTeamMemberCount();
        if (requiredCount == null || requiredCount < 2 || requiredCount > 8) {
            throw new BusinessException("该接力项目尚未配置有效的队员人数");
        }
        if (uniqueIds.size() != requiredCount) {
            throw new BusinessException("该项目要求 " + requiredCount + " 名队员");
        }
        return new ArrayList<>(uniqueIds);
    }

    private String normalizeMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return "INDIVIDUAL";
        }
        String normalized = mode.trim().toUpperCase(Locale.ROOT);
        if (!"INDIVIDUAL".equals(normalized) && !"RELAY".equals(normalized)) {
            throw new BusinessException("运动项目参赛模式配置无效");
        }
        return normalized;
    }

    private void validateStudentProfile(SysUser user) {
        if (user == null || !"STUDENT".equals(user.getRole()) || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException("仅正常状态的学生账号可以报名");
        }
        if (!StringUtils.hasText(user.getRealName())
                || !StringUtils.hasText(user.getStudentNo())
                || !StringUtils.hasText(user.getCollege())) {
            throw new BusinessException("学生姓名、学号和学院信息必须完整后才能报名");
        }
    }

    private void validateGender(SportItem item, SysUser user) {
        Integer limit = item.getGenderLimit();
        if (limit != null && limit != 0 && !limit.equals(user.getGender())) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_ALLOWED,
                    "该项目限制" + (limit == 1 ? "男性" : "女性") + "参加");
        }
    }

    private void validateSameCollege(List<SysUser> athletes) {
        if (athletes.size() < 2) {
            return;
        }
        boolean allHaveCollegeIds = athletes.stream().allMatch(user -> user.getCollegeId() != null);
        if (allHaveCollegeIds) {
            long distinctIds = athletes.stream().map(SysUser::getCollegeId).distinct().count();
            if (distinctIds != 1) {
                throw new BusinessException("接力队员必须来自同一学院");
            }
            return;
        }
        long distinctNames = athletes.stream()
                .map(user -> user.getCollege() == null ? "" : user.getCollege().trim().toLowerCase(Locale.ROOT))
                .filter(StringUtils::hasText)
                .distinct().count();
        boolean anyMissing = athletes.stream().anyMatch(user -> !StringUtils.hasText(user.getCollege()));
        if (anyMissing || distinctNames != 1) {
            throw new BusinessException("接力队员必须来自同一学院且学院信息完整");
        }
    }

    private void validateEventEntryLimits(Event event, SportItem item, List<Long> memberIds) {
        List<Registration> activeRegistrations = this.list(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getEventId, event.getId())
                .in(Registration::getStatus, PENDING, APPROVED));
        Map<Long, Registration> activeById = activeRegistrations.stream()
                .collect(Collectors.toMap(Registration::getId, registration -> registration));
        Map<Long, Set<Long>> entriesByAthlete = new HashMap<>();

        for (Registration registration : activeRegistrations) {
            entriesByAthlete.computeIfAbsent(registration.getUserId(), ignored -> new HashSet<>())
                    .add(registration.getId());
        }
        if (!activeById.isEmpty()) {
            List<RegistrationMember> members = registrationMemberMapper.selectList(
                    new LambdaQueryWrapper<RegistrationMember>()
                            .in(RegistrationMember::getRegistrationId, activeById.keySet()));
            for (RegistrationMember member : members) {
                entriesByAthlete.computeIfAbsent(member.getUserId(), ignored -> new HashSet<>())
                        .add(member.getRegistrationId());
            }
        }

        int limit = event.getMaxProjectsPerAthlete() == null ? 3 : event.getMaxProjectsPerAthlete();
        for (Long memberId : memberIds) {
            Set<Long> entryIds = entriesByAthlete.getOrDefault(memberId, Set.of());
            for (Long entryId : entryIds) {
                Registration existing = activeById.get(entryId);
                if (existing != null && item.getId().equals(existing.getSportItemId())) {
                    throw new BusinessException(ResultCode.REGISTRATION_ALREADY_EXISTS);
                }
            }
            if (limit > 0 && entryIds.size() >= limit) {
                throw new BusinessException("运动员已达到本届赛事限报项目数：" + limit);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelRegistration(Long id) {
        Registration registration = this.getById(id);
        if (registration == null) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_FOUND);
        }
        if (!registration.getUserId().equals(SecurityUtils.getUserId()) && !SecurityUtils.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (!Integer.valueOf(PENDING).equals(registration.getStatus())) {
            throw new BusinessException(ResultCode.REGISTRATION_CANNOT_CANCEL);
        }
        if (baseMapper.cancelPending(id) != 1) {
            throw new BusinessException("报名状态已变化，请刷新后重试");
        }
        sportItemMapper.releaseParticipantSlot(registration.getSportItemId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewRegistration(Long id, String status, String remark) {
        if (!SecurityUtils.isAdmin() && !SecurityUtils.isTeacher()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        Registration registration = this.getById(id);
        if (registration == null) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_FOUND);
        }
        if (!Integer.valueOf(PENDING).equals(registration.getStatus())) {
            throw new BusinessException("只能审核待审核的报名");
        }

        RegistrationStatus target;
        try {
            target = RegistrationStatus.fromApi(status);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("报名审核状态无效");
        }
        if (target != RegistrationStatus.APPROVED && target != RegistrationStatus.REJECTED) {
            throw new BusinessException("审核状态只能为 APPROVED 或 REJECTED");
        }
        String normalizedRemark = StringUtils.hasText(remark) ? remark.trim() : null;
        if (target == RegistrationStatus.REJECTED && normalizedRemark == null) {
            throw new BusinessException("拒绝报名必须填写拒绝原因");
        }
        if (normalizedRemark != null && normalizedRemark.length() > 255) {
            throw new BusinessException("审核意见最多255个字符");
        }

        int changed = baseMapper.reviewPending(id, target.getCode(), SecurityUtils.getUserId(),
                LocalDateTime.now(), normalizedRemark);
        if (changed != 1) {
            throw new BusinessException("报名状态已变化，请刷新后重试");
        }
        if (target == RegistrationStatus.REJECTED) {
            sportItemMapper.releaseParticipantSlot(registration.getSportItemId());
        }
    }

    @Override
    public IPage<RegistrationVO> pageRegistrations(Integer current, Integer size, Long eventId,
                                                    Long sportItemId, String status) {
        validatePage(current, size);
        Page<Registration> page = new Page<>(current, size);
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<Registration>()
                .eq(eventId != null, Registration::getEventId, eventId)
                .eq(sportItemId != null, Registration::getSportItemId, sportItemId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(Registration::getStatus, parseStatusCode(status));
        }
        wrapper.orderByDesc(Registration::getCreateTime);
        return convertToVOPage(this.page(page, wrapper));
    }

    @Override
    public IPage<RegistrationVO> getMyRegistrations(Integer current, Integer size, String status) {
        validatePage(current, size);
        Long userId = SecurityUtils.getUserId();
        List<RegistrationMember> memberships = registrationMemberMapper.selectList(
                new LambdaQueryWrapper<RegistrationMember>().eq(RegistrationMember::getUserId, userId));
        Set<Long> memberRegistrationIds = memberships.stream()
                .map(RegistrationMember::getRegistrationId).collect(Collectors.toSet());

        Page<Registration> page = new Page<>(current, size);
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        if (memberRegistrationIds.isEmpty()) {
            wrapper.eq(Registration::getUserId, userId);
        } else {
            wrapper.and(query -> query.eq(Registration::getUserId, userId)
                    .or().in(Registration::getId, memberRegistrationIds));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Registration::getStatus, parseStatusCode(status));
        }
        wrapper.orderByDesc(Registration::getCreateTime);
        return convertToVOPage(this.page(page, wrapper));
    }

    @Override
    public List<RegistrationMemberVO> listEligibleAthletes(String keyword) {
        SysUser current = sysUserService.getById(SecurityUtils.getUserId());
        validateStudentProfile(current);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "STUDENT")
                .eq(SysUser::getStatus, 1);
        if (current.getCollegeId() != null && StringUtils.hasText(current.getCollege())) {
            wrapper.and(query -> query.eq(SysUser::getCollegeId, current.getCollegeId())
                    .or().eq(SysUser::getCollege, current.getCollege()));
        } else if (StringUtils.hasText(current.getCollege())) {
            wrapper.eq(SysUser::getCollege, current.getCollege());
        } else {
            throw new BusinessException("请先完善学院信息后再选择接力队员");
        }
        if (StringUtils.hasText(keyword)) {
            String search = keyword.trim();
            wrapper.and(query -> query.like(SysUser::getRealName, search)
                    .or().like(SysUser::getStudentNo, search));
        }
        wrapper.orderByAsc(SysUser::getRealName).last("LIMIT 50");

        return sysUserService.list(wrapper).stream().map(user -> {
            RegistrationMemberVO vo = new RegistrationMemberVO();
            vo.setUserId(user.getId());
            vo.setRealName(user.getRealName());
            vo.setStudentNo(user.getStudentNo());
            vo.setCollege(user.getCollege());
            vo.setClassName(user.getClassName());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean hasRegistered(Long eventId, Long sportItemId, Long userId) {
        List<Registration> active = this.list(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getEventId, eventId)
                .eq(Registration::getSportItemId, sportItemId)
                .in(Registration::getStatus, PENDING, APPROVED));
        if (active.stream().anyMatch(registration -> userId.equals(registration.getUserId()))) {
            return true;
        }
        if (active.isEmpty()) {
            return false;
        }
        Set<Long> registrationIds = active.stream().map(Registration::getId).collect(Collectors.toSet());
        return registrationMemberMapper.selectCount(new LambdaQueryWrapper<RegistrationMember>()
                .in(RegistrationMember::getRegistrationId, registrationIds)
                .eq(RegistrationMember::getUserId, userId)) > 0;
    }

    private int parseStatusCode(String status) {
        String normalized = status.trim();
        if (normalized.matches("[1-4]")) {
            return Integer.parseInt(normalized);
        }
        try {
            RegistrationStatus parsed = RegistrationStatus.fromApi(normalized);
            if (parsed == null) {
                throw new IllegalArgumentException("empty");
            }
            return parsed.getCode();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("报名状态筛选值无效");
        }
    }

    private void validatePage(Integer current, Integer size) {
        if (current == null || current < 1 || size == null || size < 1 || size > 100) {
            throw new BusinessException("分页参数无效：页码必须大于0，每页数量为1至100");
        }
    }

    private IPage<RegistrationVO> convertToVOPage(IPage<Registration> page) {
        Page<RegistrationVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(convertToVOList(page.getRecords()));
        return voPage;
    }

    private List<RegistrationVO> convertToVOList(List<Registration> registrations) {
        if (registrations.isEmpty()) {
            return List.of();
        }

        Set<Long> eventIds = registrations.stream().map(Registration::getEventId).collect(Collectors.toSet());
        Set<Long> sportItemIds = registrations.stream().map(Registration::getSportItemId).collect(Collectors.toSet());
        Set<Long> userIds = registrations.stream().map(Registration::getUserId).collect(Collectors.toSet());
        Set<Long> reviewerIds = registrations.stream().map(Registration::getReviewerId)
                .filter(id -> id != null).collect(Collectors.toSet());
        userIds.addAll(reviewerIds);
        Set<Long> registrationIds = registrations.stream().map(Registration::getId).collect(Collectors.toSet());

        List<RegistrationMember> memberRows = registrationMemberMapper.selectList(
                new LambdaQueryWrapper<RegistrationMember>()
                        .in(RegistrationMember::getRegistrationId, registrationIds)
                        .orderByAsc(RegistrationMember::getMemberOrder));
        memberRows.stream().map(RegistrationMember::getUserId).forEach(userIds::add);

        Map<Long, Event> eventMap = eventService.listByIds(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, event -> event));
        Map<Long, SportItem> itemMap = sportItemService.listByIds(sportItemIds).stream()
                .collect(Collectors.toMap(SportItem::getId, item -> item));
        Map<Long, SysUser> userMap = sysUserService.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, user -> user, (left, right) -> left));
        Map<Long, List<RegistrationMember>> membersByRegistration = memberRows.stream()
                .collect(Collectors.groupingBy(RegistrationMember::getRegistrationId));

        return registrations.stream().map(registration -> {
            RegistrationVO vo = new RegistrationVO();
            BeanUtils.copyProperties(registration, vo);
            RegistrationStatus status = RegistrationStatus.fromCode(registration.getStatus());
            vo.setStatus(status == null ? "" : status.name());
            vo.setStatusName(status == null ? "" : status.getLabel());

            Event event = eventMap.get(registration.getEventId());
            if (event != null) {
                vo.setEventName(event.getName());
            }
            SportItem item = itemMap.get(registration.getSportItemId());
            if (item != null) {
                vo.setSportItemName(item.getName());
            }
            SysUser applicant = userMap.get(registration.getUserId());
            if (applicant != null) {
                vo.setUsername(applicant.getUsername());
                vo.setRealName(applicant.getRealName());
                vo.setStudentNo(applicant.getStudentNo());
                vo.setCollege(applicant.getCollege());
                vo.setClassName(applicant.getClassName());
                vo.setPhone(applicant.getPhone());
                if (!StringUtils.hasText(vo.getCollegeSnapshot())) {
                    vo.setCollegeSnapshot(applicant.getCollege());
                }
                if (!StringUtils.hasText(vo.getClassSnapshot())) {
                    vo.setClassSnapshot(applicant.getClassName());
                }
            }
            if (registration.getReviewerId() != null) {
                SysUser reviewer = userMap.get(registration.getReviewerId());
                if (reviewer != null) {
                    vo.setReviewerName(reviewer.getRealName());
                }
            }
            if (!StringUtils.hasText(vo.getRegistrationType())) {
                vo.setRegistrationType("INDIVIDUAL");
            }

            List<RegistrationMemberVO> memberVOs = membersByRegistration
                    .getOrDefault(registration.getId(), List.of()).stream().map(member -> {
                        RegistrationMemberVO memberVO = new RegistrationMemberVO();
                        memberVO.setUserId(member.getUserId());
                        memberVO.setMemberOrder(member.getMemberOrder());
                        SysUser athlete = userMap.get(member.getUserId());
                        if (athlete != null) {
                            memberVO.setRealName(athlete.getRealName());
                            memberVO.setStudentNo(athlete.getStudentNo());
                            memberVO.setCollege(StringUtils.hasText(member.getCollegeSnapshot())
                                    ? member.getCollegeSnapshot() : athlete.getCollege());
                            memberVO.setClassName(StringUtils.hasText(member.getClassSnapshot())
                                    ? member.getClassSnapshot() : athlete.getClassName());
                        } else {
                            memberVO.setCollege(member.getCollegeSnapshot());
                            memberVO.setClassName(member.getClassSnapshot());
                        }
                        return memberVO;
                    }).collect(Collectors.toList());
            vo.setMembers(memberVOs);
            return vo;
        }).collect(Collectors.toList());
    }
}
