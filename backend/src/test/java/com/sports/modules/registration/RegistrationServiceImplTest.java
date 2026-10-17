package com.sports.modules.registration;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.sports.common.exception.BusinessException;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.service.EventService;
import com.sports.modules.registration.dto.RegistrationDTO;
import com.sports.modules.registration.entity.Registration;
import com.sports.modules.registration.mapper.RegistrationMapper;
import com.sports.modules.registration.mapper.RegistrationMemberMapper;
import com.sports.modules.registration.service.impl.RegistrationServiceImpl;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.mapper.SportItemMapper;
import com.sports.modules.sportitem.service.SportItemService;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.mapper.SysUserMapper;
import com.sports.modules.system.service.SysUserService;
import com.sports.security.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationServiceImplTest {

    @Mock private EventService eventService;
    @Mock private SportItemService sportItemService;
    @Mock private SportItemMapper sportItemMapper;
    @Mock private SysUserService sysUserService;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private RegistrationMemberMapper registrationMemberMapper;
    @Mock private RegistrationMapper registrationMapper;

    private RegistrationServiceImpl service;
    private MockedStatic<SecurityUtils> security;

    @BeforeEach
    void setUp() {
        service = new RegistrationServiceImpl(eventService, sportItemService, sportItemMapper,
                sysUserService, sysUserMapper, registrationMemberMapper);
        ReflectionTestUtils.setField(service, "baseMapper", registrationMapper);

        security = mockStatic(SecurityUtils.class);
        security.when(SecurityUtils::getUserId).thenReturn(10L);

        when(sysUserMapper.lockForRegistration(anyLong())).thenAnswer(invocation -> invocation.getArgument(0));
        when(registrationMapper.selectList(any())).thenReturn(List.of());
        when(registrationMapper.insert(any(Registration.class))).thenAnswer(invocation -> {
            Registration registration = invocation.getArgument(0);
            registration.setId(900L);
            return 1;
        });
        when(registrationMemberMapper.selectList(any())).thenReturn(List.of());
        when(registrationMemberMapper.insert(any())).thenReturn(1);
        when(sportItemMapper.reserveParticipantSlot(anyLong())).thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        if (security != null) {
            security.close();
        }
    }

    @Test
    void individualRegistrationKeepsLegacyPayloadAndDoesNotCreateRosterRows() {
        Event event = activeEvent(3);
        SportItem item = activeItem("INDIVIDUAL", null);
        SysUser applicant = student(10L);
        when(eventService.getById(100L)).thenReturn(event);
        when(sportItemService.getById(200L)).thenReturn(item);
        when(sysUserService.listByIds(any())).thenReturn(List.of(applicant));

        RegistrationDTO dto = new RegistrationDTO();
        dto.setEventId(100L);
        dto.setSportItemId(200L);
        dto.setRemark("个人报名");

        assertEquals(900L, service.register(dto));
        verify(registrationMapper).insert(any(Registration.class));
        verify(registrationMemberMapper, never()).insert(any());
        verify(sportItemMapper).reserveParticipantSlot(200L);
    }

    @Test
    void relayRegistrationPersistsEveryMemberInSubmittedOrder() {
        Event event = activeEvent(3);
        SportItem item = activeItem("RELAY", 4);
        List<SysUser> athletes = List.of(student(10L), student(11L), student(12L), student(13L));
        when(eventService.getById(100L)).thenReturn(event);
        when(sportItemService.getById(200L)).thenReturn(item);
        when(sysUserService.listByIds(any())).thenReturn(athletes);

        RegistrationDTO dto = new RegistrationDTO();
        dto.setEventId(100L);
        dto.setSportItemId(200L);
        dto.setTeamName("体育学院接力队");
        dto.setMemberUserIds(List.of(10L, 11L, 12L, 13L));

        assertEquals(900L, service.register(dto));
        verify(registrationMemberMapper, times(4)).insert(any());
        verify(sportItemMapper).reserveParticipantSlot(200L);
    }

    @Test
    void relayRegistrationRejectsMembersFromDifferentColleges() {
        Event event = activeEvent(3);
        SportItem item = activeItem("RELAY", 4);
        SysUser secondAthlete = student(11L);
        secondAthlete.setCollegeId(2L);
        secondAthlete.setCollege("工程学院");
        when(eventService.getById(100L)).thenReturn(event);
        when(sportItemService.getById(200L)).thenReturn(item);
        when(sysUserService.listByIds(any())).thenReturn(
                List.of(student(10L), secondAthlete, student(12L), student(13L)));

        RegistrationDTO dto = new RegistrationDTO();
        dto.setEventId(100L);
        dto.setSportItemId(200L);
        dto.setTeamName("跨学院接力队");
        dto.setMemberUserIds(List.of(10L, 11L, 12L, 13L));

        assertThrows(BusinessException.class, () -> service.register(dto));
        verify(sportItemMapper, never()).reserveParticipantSlot(anyLong());
    }

    @Test
    void relayRegistrationRejectsAthletesWhoFailGenderRestriction() {
        Event event = activeEvent(3);
        SportItem item = activeItem("RELAY", 4);
        item.setGenderLimit(1);
        SysUser femaleAthlete = student(12L);
        femaleAthlete.setGender(2);
        when(eventService.getById(100L)).thenReturn(event);
        when(sportItemService.getById(200L)).thenReturn(item);
        when(sysUserService.listByIds(any())).thenReturn(
                List.of(student(10L), student(11L), femaleAthlete, student(13L)));

        RegistrationDTO dto = new RegistrationDTO();
        dto.setEventId(100L);
        dto.setSportItemId(200L);
        dto.setTeamName("男子接力队");
        dto.setMemberUserIds(List.of(10L, 11L, 12L, 13L));

        assertThrows(BusinessException.class, () -> service.register(dto));
        verify(sportItemMapper, never()).reserveParticipantSlot(anyLong());
    }

    @Test
    void relayRegistrationRejectsIncompleteRosterBeforeReservingCapacity() {
        when(eventService.getById(100L)).thenReturn(activeEvent(3));
        when(sportItemService.getById(200L)).thenReturn(activeItem("RELAY", 4));

        RegistrationDTO dto = new RegistrationDTO();
        dto.setEventId(100L);
        dto.setSportItemId(200L);
        dto.setTeamName("体育学院接力队");
        dto.setMemberUserIds(List.of(10L, 11L, 12L));

        assertThrows(BusinessException.class, () -> service.register(dto));
        verify(sportItemMapper, never()).reserveParticipantSlot(anyLong());
        verify(registrationMapper, never()).insert(any(Registration.class));
    }

    @Test
    void eventEntryLimitIsEnforcedBeforeCapacityReservation() {
        Event event = activeEvent(1);
        SportItem item = activeItem("INDIVIDUAL", null);
        SysUser applicant = student(10L);
        Registration existing = new Registration();
        existing.setId(700L);
        existing.setEventId(100L);
        existing.setSportItemId(201L);
        existing.setUserId(10L);
        existing.setStatus(2);

        when(eventService.getById(100L)).thenReturn(event);
        when(sportItemService.getById(200L)).thenReturn(item);
        when(sysUserService.listByIds(any())).thenReturn(List.of(applicant));
        when(registrationMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));

        RegistrationDTO dto = new RegistrationDTO();
        dto.setEventId(100L);
        dto.setSportItemId(200L);

        assertThrows(BusinessException.class, () -> service.register(dto));
        verify(sportItemMapper, never()).reserveParticipantSlot(anyLong());
        verify(registrationMapper, never()).insert(any(Registration.class));
    }

    @Test
    void rejectingRegistrationRequiresReasonAndReleasesReservedSlotOnce() {
        Registration pending = new Registration();
        pending.setId(500L);
        pending.setEventId(100L);
        pending.setSportItemId(200L);
        pending.setUserId(10L);
        pending.setStatus(1);
        when(registrationMapper.selectById(500L)).thenReturn(pending);
        security.when(SecurityUtils::isTeacher).thenReturn(true);
        when(registrationMapper.reviewPending(eq(500L), eq(3), eq(10L), any(LocalDateTime.class), eq("资格不符")))
                .thenReturn(1);

        assertThrows(BusinessException.class, () -> service.reviewRegistration(500L, "REJECTED", " "));
        service.reviewRegistration(500L, "REJECTED", "资格不符");

        verify(registrationMapper, times(1))
                .reviewPending(eq(500L), eq(3), eq(10L), any(LocalDateTime.class), eq("资格不符"));
        verify(sportItemMapper, times(1)).releaseParticipantSlot(200L);
    }

    private Event activeEvent(int projectLimit) {
        Event event = new Event();
        event.setId(100L);
        event.setStatus(2);
        event.setRegStartDate(LocalDate.now().minusDays(1));
        event.setRegEndDate(LocalDate.now().plusDays(1));
        event.setMaxProjectsPerAthlete(projectLimit);
        return event;
    }

    private SportItem activeItem(String mode, Integer teamMemberCount) {
        SportItem item = new SportItem();
        item.setId(200L);
        item.setEventId(100L);
        item.setStatus(1);
        item.setParticipationMode(mode);
        item.setTeamMemberCount(teamMemberCount);
        item.setGenderLimit(0);
        item.setMaxParticipants(20);
        item.setCurrentParticipants(0);
        return item;
    }

    private SysUser student(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setRole("STUDENT");
        user.setStatus(1);
        user.setRealName("运动员" + id);
        user.setStudentNo("S" + id);
        user.setCollege("体育学院");
        user.setCollegeId(1L);
        user.setClassName("体育班");
        user.setClassId(10L);
        user.setGender(1);
        return user;
    }
}
