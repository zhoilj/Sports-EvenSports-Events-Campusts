package com.sports.modules.registration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.ResultCode;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.service.EventService;
import com.sports.modules.registration.dto.RelayTeamCreateDTO;
import com.sports.modules.registration.entity.RelayTeam;
import com.sports.modules.registration.entity.RelayTeamMember;
import com.sports.modules.registration.mapper.RelayTeamMapper;
import com.sports.modules.registration.mapper.RelayTeamMemberMapper;
import com.sports.modules.registration.service.RelayTeamService;
import com.sports.modules.registration.vo.RelayTeamMemberVO;
import com.sports.modules.registration.vo.RelayTeamVO;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RelayTeamServiceImpl extends ServiceImpl<RelayTeamMapper, RelayTeam> implements RelayTeamService {
    private final RelayTeamMemberMapper memberMapper;
    private final EventService eventService;
    private final SportItemService sportItemService;
    private final SysUserService sysUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTeam(RelayTeamCreateDTO dto) {
        if (dto == null || dto.getEventId() == null || dto.getSportItemId() == null) {
            throw new BusinessException("赛事和接力项目不能为空");
        }
        Long currentUserId = SecurityUtils.getUserId();
        SysUser captain = requireActiveStudent(currentUserId);
        if (!StringUtils.hasText(captain.getCollege())) {
            throw new BusinessException("请先完善个人资料中的学院信息，再创建接力队伍");
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

        // Serialize team capacity checks for a single relay event.
        SportItem item = sportItemService.getOne(new LambdaQueryWrapper<SportItem>()
                .eq(SportItem::getId, dto.getSportItemId())
                .last("FOR UPDATE"));
        validateRelayItem(item, dto.getEventId());

        long activeTeams = this.count(new LambdaQueryWrapper<RelayTeam>()
                .eq(RelayTeam::getEventId, dto.getEventId())
                .eq(RelayTeam::getSportItemId, dto.getSportItemId())
                .in(RelayTeam::getStatus, 1, 2));
        Integer maxTeams = item.getMaxParticipants();
        if (maxTeams != null && maxTeams > 0 && activeTeams >= maxTeams) {
            throw new BusinessException(ResultCode.REGISTRATION_LIMIT_EXCEEDED);
        }

        List<Long> roster = normalizeRoster(dto.getMemberUserIds(), currentUserId, item);
        validateRosterUsers(roster, captain.getCollege(), item, captain.getClassName());
        roster = arrangeMixedRelayRoster(roster, item);
        ensureMembersAvailable(roster, dto.getEventId(), dto.getSportItemId(), null);

        RelayTeam team = new RelayTeam();
        team.setEventId(dto.getEventId());
        team.setSportItemId(dto.getSportItemId());
        team.setTeamName(dto.getTeamName().trim());
        team.setCollege(captain.getCollege());
        team.setClassName(captain.getClassName());
        team.setCreatorUserId(currentUserId);
        team.setStatus(1);
        this.save(team);
        saveRoster(team.getId(), roster, item.getRelayMemberCount() == null ? 4 : item.getRelayMemberCount());
        refreshRelayTeamCount(item);
        return team.getId();
    }

    @Override
    public IPage<RelayTeamVO> pageTeams(Integer current, Integer size, Long eventId, Long sportItemId,
                                       Integer status, String college) {
        Page<RelayTeam> page = new Page<>(current, size);
        LambdaQueryWrapper<RelayTeam> wrapper = new LambdaQueryWrapper<RelayTeam>()
                .eq(eventId != null, RelayTeam::getEventId, eventId)
                .eq(sportItemId != null, RelayTeam::getSportItemId, sportItemId)
                .eq(status != null, RelayTeam::getStatus, status)
                .like(StringUtils.hasText(college), RelayTeam::getCollege, college)
                .orderByDesc(RelayTeam::getCreateTime);
        return this.page(page, wrapper).convert(this::toVO);
    }

    @Override
    public List<RelayTeamVO> getMyTeams() {
        Long userId = SecurityUtils.getUserId();
        List<Long> memberTeamIds = memberMapper.selectList(new LambdaQueryWrapper<RelayTeamMember>()
                        .eq(RelayTeamMember::getUserId, userId))
                .stream().map(RelayTeamMember::getTeamId).distinct().collect(Collectors.toList());
        LambdaQueryWrapper<RelayTeam> wrapper = new LambdaQueryWrapper<RelayTeam>()
                .eq(RelayTeam::getCreatorUserId, userId);
        if (!memberTeamIds.isEmpty()) {
            wrapper.or().in(RelayTeam::getId, memberTeamIds);
        }
        return this.list(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public RelayTeamVO getTeamDetail(Long id) {
        RelayTeam team = this.getById(id);
        if (team == null) {
            throw new BusinessException("接力队伍不存在");
        }
        Long userId = SecurityUtils.getUserId();
        SysUser current = sysUserService.getById(userId);
        boolean staff = current != null && ("ADMIN".equals(current.getRole()) || "TEACHER".equals(current.getRole()));
        boolean member = memberMapper.selectCount(new LambdaQueryWrapper<RelayTeamMember>()
                .eq(RelayTeamMember::getTeamId, id)
                .eq(RelayTeamMember::getUserId, userId)) > 0;
        if (!staff && !team.getCreatorUserId().equals(userId) && !member) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return toVO(team);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewTeam(Long id, Integer status, String remark) {
        if (status == null || (status != 2 && status != 3)) {
            throw new BusinessException("审核状态只能为通过或拒绝");
        }
        RelayTeam team = this.getById(id);
        if (team == null) {
            throw new BusinessException("接力队伍不存在");
        }
        if (team.getStatus() == null || team.getStatus() != 1) {
            throw new BusinessException("只能审核待审核的接力队伍");
        }
        team.setStatus(status);
        team.setReviewerId(SecurityUtils.getUserId());
        team.setReviewTime(LocalDateTime.now());
        team.setReviewRemark(remark);
        this.updateById(team);
        if (status == 3) {
            SportItem item = sportItemService.getById(team.getSportItemId());
            if (item != null) refreshRelayTeamCount(item);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTeam(Long id) {
        RelayTeam team = this.getById(id);
        if (team == null) throw new BusinessException("接力队伍不存在");
        Long userId = SecurityUtils.getUserId();
        if (!team.getCreatorUserId().equals(userId) && !SecurityUtils.isAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (team.getStatus() == null || team.getStatus() != 1) {
            throw new BusinessException("仅待审核的接力队伍可以取消");
        }
        team.setStatus(4);
        this.updateById(team);
        SportItem item = sportItemService.getById(team.getSportItemId());
        if (item != null) refreshRelayTeamCount(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceMembers(Long id, List<Long> memberUserIds) {
        RelayTeam team = this.getById(id);
        if (team == null) throw new BusinessException("接力队伍不存在");
        if (!team.getCreatorUserId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (team.getStatus() == null || team.getStatus() != 1) {
            throw new BusinessException("队伍审核通过后不能修改队员");
        }
        SportItem item = sportItemService.getById(team.getSportItemId());
        validateRelayItem(item, team.getEventId());
        List<Long> roster = normalizeRoster(memberUserIds, team.getCreatorUserId(), item);
        validateRosterUsers(roster, team.getCollege(), item, team.getClassName());
        roster = arrangeMixedRelayRoster(roster, item);
        ensureMembersAvailable(roster, team.getEventId(), team.getSportItemId(), team.getId());
        memberMapper.delete(new LambdaQueryWrapper<RelayTeamMember>().eq(RelayTeamMember::getTeamId, id));
        saveRoster(id, roster, item.getRelayMemberCount() == null ? 4 : item.getRelayMemberCount());
    }

    @Override
    public List<RelayTeamMemberVO> getEligibleMembers(Long sportItemId, Long teamId, String keyword) {
        SysUser captain = requireActiveStudent(SecurityUtils.getUserId());
        if (!StringUtils.hasText(captain.getCollege())) {
            throw new BusinessException("请先完善个人资料中的学院信息");
        }
        Set<Long> occupiedUserIds = new HashSet<>();
        if (sportItemId != null) {
            List<Long> occupiedTeamIds = this.list(new LambdaQueryWrapper<RelayTeam>()
                            .eq(RelayTeam::getSportItemId, sportItemId)
                            .ne(teamId != null, RelayTeam::getId, teamId)
                            .in(RelayTeam::getStatus, 1, 2))
                    .stream().map(RelayTeam::getId).collect(Collectors.toList());
            if (!occupiedTeamIds.isEmpty()) {
                occupiedUserIds.addAll(memberMapper.selectList(new LambdaQueryWrapper<RelayTeamMember>()
                                .in(RelayTeamMember::getTeamId, occupiedTeamIds))
                        .stream().map(RelayTeamMember::getUserId).collect(Collectors.toSet()));
            }
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "STUDENT")
                .eq(SysUser::getStatus, 1)
                .eq(SysUser::getCollege, captain.getCollege())
                .notIn(!occupiedUserIds.isEmpty(), SysUser::getId, occupiedUserIds)
                .and(StringUtils.hasText(keyword), q -> q.like(SysUser::getRealName, keyword)
                        .or().like(SysUser::getStudentNo, keyword)
                        .or().like(SysUser::getClassName, keyword))
                .orderByAsc(SysUser::getClassName)
                .orderByAsc(SysUser::getStudentNo);
        return sysUserService.list(wrapper).stream().map(this::memberSearchVO).collect(Collectors.toList());
    }

    private SysUser requireActiveStudent(Long userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1
                || !"STUDENT".equals(user.getRole())) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_ALLOWED, "仅状态正常的学生账号可以创建接力队伍");
        }
        return user;
    }

    private void validateRelayItem(SportItem item, Long eventId) {
        if (item == null || !java.util.Objects.equals(item.getEventId(), eventId)) {
            throw new BusinessException(ResultCode.SPORT_ITEM_NOT_FOUND);
        }
        if (item.getStatus() == null || item.getStatus() != 1) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_ALLOWED, "该项目当前未开放报名");
        }
        if (item.getIsRelay() == null || item.getIsRelay() != 1) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_ALLOWED, "所选项目不是接力项目");
        }
    }

    private List<Long> normalizeRoster(List<Long> requestedIds, Long creatorId, SportItem item) {
        if (requestedIds == null) throw new BusinessException("请登记接力队员");
        List<Long> roster = new ArrayList<>(requestedIds);
        if (roster.stream().anyMatch(java.util.Objects::isNull)) {
            throw new BusinessException("队员列表包含无效用户");
        }
        if (new HashSet<>(roster).size() != roster.size()) {
            throw new BusinessException("同一队伍不能重复登记同一名队员");
        }
        if (!roster.contains(creatorId)) roster.add(0, creatorId);
        int required = item.getRelayMemberCount() == null ? 4 : item.getRelayMemberCount();
        int reserves = item.getRelayReserveCount() == null ? 0 : item.getRelayReserveCount();
        if (roster.size() < required || roster.size() > required + reserves) {
            throw new BusinessException("该项目要求" + required + "名正式队员，最多另加" + reserves + "名替补队员");
        }
        return roster;
    }

    private void validateRosterUsers(List<Long> roster, String college, SportItem item, String captainClass) {
        for (Long userId : roster) {
            SysUser user = requireActiveStudent(userId);
            if (!StringUtils.hasText(user.getCollege()) || !user.getCollege().equals(college)) {
                throw new BusinessException("接力队员必须来自队伍所属学院");
            }
            if ("B".equalsIgnoreCase(item.getDivision()) && StringUtils.hasText(captainClass)
                    && !captainClass.equals(user.getClassName())) {
                throw new BusinessException("乙组接力队员必须来自同一年级/专业班级，团体积分按班级归属统计");
            }
        }
    }

    private List<Long> arrangeMixedRelayRoster(List<Long> roster, SportItem item) {
        if (item == null || !("MIXED".equalsIgnoreCase(item.getGenderCategory())
                || (item.getName() != null && item.getName().contains("混合接力")))) {
            return roster;
        }
        int required = item.getRelayMemberCount() == null ? 4 : item.getRelayMemberCount();
        if (required != 4) throw new BusinessException("混合接力必须配置4名正式队员");
        List<Long> women = new ArrayList<>();
        List<Long> men = new ArrayList<>();
        for (Long userId : roster) {
            SysUser user = sysUserService.getById(userId);
            if (user == null) throw new BusinessException("接力队员账号不存在");
            if (Objects.equals(user.getGender(), 2)) women.add(userId);
            else if (Objects.equals(user.getGender(), 1)) men.add(userId);
        }
        if (women.size() < 2 || men.size() < 2) {
            throw new BusinessException("混合接力正式队员须至少2男2女，且第1、3棒为女生，第2、4棒为男生");
        }
        List<Long> arranged = new ArrayList<>();
        arranged.add(women.get(0)); // Leg 1: female
        arranged.add(men.get(0));   // Leg 2: male
        arranged.add(women.get(1)); // Leg 3: female
        arranged.add(men.get(1));   // Leg 4: male
        Set<Long> formal = new HashSet<>(arranged);
        for (Long userId : roster) {
            if (!formal.contains(userId)) arranged.add(userId);
        }
        return arranged;
    }

    private void ensureMembersAvailable(List<Long> roster, Long eventId, Long sportItemId, Long excludedTeamId) {
        if (roster.isEmpty()) return;
        List<RelayTeamMember> memberships = memberMapper.selectList(new LambdaQueryWrapper<RelayTeamMember>()
                .in(RelayTeamMember::getUserId, roster));
        Set<Long> teamIds = memberships.stream().map(RelayTeamMember::getTeamId).collect(Collectors.toSet());
        if (teamIds.isEmpty()) return;
        List<RelayTeam> existingTeams = this.list(new LambdaQueryWrapper<RelayTeam>()
                .in(RelayTeam::getId, teamIds)
                .eq(RelayTeam::getEventId, eventId)
                .eq(RelayTeam::getSportItemId, sportItemId)
                .in(RelayTeam::getStatus, 1, 2));
        for (RelayTeam existing : existingTeams) {
            if (excludedTeamId == null || !excludedTeamId.equals(existing.getId())) {
                throw new BusinessException("队员已参加该项目的其他待审或已通过队伍");
            }
        }
    }

    private void saveRoster(Long teamId, List<Long> roster, int formalCount) {
        List<RelayTeamMember> members = new ArrayList<>();
        for (int i = 0; i < roster.size(); i++) {
            RelayTeamMember member = new RelayTeamMember();
            member.setTeamId(teamId);
            member.setUserId(roster.get(i));
            member.setLegOrder(i + 1);
            member.setReserve(i >= formalCount ? 1 : 0);
            members.add(member);
        }
        for (RelayTeamMember member : members) {
            memberMapper.insert(member);
        }
    }

    private void refreshRelayTeamCount(SportItem item) {
        long activeTeams = this.count(new LambdaQueryWrapper<RelayTeam>()
                .eq(RelayTeam::getSportItemId, item.getId())
                .in(RelayTeam::getStatus, 1, 2));
        item.setCurrentParticipants(Math.toIntExact(activeTeams));
        sportItemService.updateById(item);
    }

    private RelayTeamVO toVO(RelayTeam team) {
        RelayTeamVO vo = new RelayTeamVO();
        BeanUtils.copyProperties(team, vo);
        Event event = eventService.getById(team.getEventId());
        if (event != null) vo.setEventName(event.getName());
        SportItem item = sportItemService.getById(team.getSportItemId());
        if (item != null) vo.setSportItemName(item.getName());
        SysUser creator = sysUserService.getById(team.getCreatorUserId());
        if (creator != null) vo.setCreatorName(creator.getRealName());
        if (team.getReviewerId() != null) {
            SysUser reviewer = sysUserService.getById(team.getReviewerId());
            if (reviewer != null) vo.setReviewerName(reviewer.getRealName());
        }
        vo.setStatusName(switch (team.getStatus() == null ? 0 : team.getStatus()) {
            case 1 -> "待审核";
            case 2 -> "已通过";
            case 3 -> "已拒绝";
            case 4 -> "已取消";
            default -> "未知";
        });
        List<RelayTeamMemberVO> members = memberMapper.selectList(new LambdaQueryWrapper<RelayTeamMember>()
                        .eq(RelayTeamMember::getTeamId, team.getId())
                        .orderByAsc(RelayTeamMember::getLegOrder))
                .stream().map(this::toMemberVO).collect(Collectors.toList());
        vo.setMembers(members);
        return vo;
    }

    private RelayTeamMemberVO toMemberVO(RelayTeamMember member) {
        RelayTeamMemberVO vo = new RelayTeamMemberVO();
        vo.setUserId(member.getUserId());
        vo.setLegOrder(member.getLegOrder());
        vo.setReserve(member.getReserve() != null && member.getReserve() == 1);
        SysUser user = sysUserService.getById(member.getUserId());
        if (user != null) {
            vo.setRealName(user.getRealName());
            vo.setUsername(user.getUsername());
            vo.setStudentNo(user.getStudentNo());
            vo.setCollege(user.getCollege());
            vo.setClassName(user.getClassName());
        }
        return vo;
    }

    private RelayTeamMemberVO memberSearchVO(SysUser user) {
        RelayTeamMemberVO vo = new RelayTeamMemberVO();
        vo.setUserId(user.getId());
        vo.setRealName(user.getRealName());
        vo.setUsername(user.getUsername());
        vo.setStudentNo(user.getStudentNo());
        vo.setCollege(user.getCollege());
        vo.setClassName(user.getClassName());
        return vo;
    }
}
