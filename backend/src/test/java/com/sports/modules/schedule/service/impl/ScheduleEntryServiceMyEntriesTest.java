package com.sports.modules.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.service.EventService;
import com.sports.modules.registration.entity.Registration;
import com.sports.modules.registration.entity.RelayTeam;
import com.sports.modules.registration.entity.RelayTeamMember;
import com.sports.modules.registration.mapper.RelayTeamMemberMapper;
import com.sports.modules.registration.service.RegistrationService;
import com.sports.modules.registration.service.RelayTeamService;
import com.sports.modules.result.entity.CompetitionResult;
import com.sports.modules.result.service.CompetitionResultService;
import com.sports.modules.schedule.entity.CheckinRecord;
import com.sports.modules.schedule.entity.Schedule;
import com.sports.modules.schedule.entity.ScheduleEntry;
import com.sports.modules.schedule.mapper.CheckinRecordMapper;
import com.sports.modules.schedule.mapper.ScheduleEntryMapper;
import com.sports.modules.schedule.service.ScheduleService;
import com.sports.modules.schedule.vo.ScheduleEntryVO;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.service.SportItemService;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.service.SysUserService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleEntryServiceMyEntriesTest {

    private static final Long USER_ID = 10L;
    private static final Long OTHER_USER_ID = 20L;
    private static final Long SCHEDULE_ID = 900L;
    private static final Long EVENT_ID = 2L;
    private static final Long SPORT_ITEM_ID = 3L;

    @Mock private ScheduleService scheduleService;
    @Mock private EventService eventService;
    @Mock private SportItemService sportItemService;
    @Mock private RegistrationService registrationService;
    @Mock private RelayTeamService relayTeamService;
    @Mock private RelayTeamMemberMapper relayTeamMemberMapper;
    @Mock private CheckinRecordMapper checkinRecordMapper;
    @Mock private CompetitionResultService competitionResultService;
    @Mock private SysUserService sysUserService;
    @Mock private ScheduleEntryMapper scheduleEntryMapper;

    private ScheduleEntryServiceImpl service;

    @BeforeAll
    static void initializeMyBatisLambdaMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        initializeTableInfo(configuration, Registration.class);
        initializeTableInfo(configuration, RelayTeamMember.class);
        initializeTableInfo(configuration, RelayTeam.class);
        initializeTableInfo(configuration, ScheduleEntry.class);
        initializeTableInfo(configuration, CheckinRecord.class);
        initializeTableInfo(configuration, CompetitionResult.class);
    }

    private static void initializeTableInfo(MybatisConfiguration configuration, Class<?> entityType) {
        if (TableInfoHelper.getTableInfo(entityType) != null) return;
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "unit-test");
        assistant.setCurrentNamespace("unit-test." + entityType.getSimpleName());
        TableInfoHelper.initTableInfo(assistant, entityType);
    }

    @BeforeEach
    void setUp() {
        service = new ScheduleEntryServiceImpl(
                scheduleService,
                eventService,
                sportItemService,
                registrationService,
                relayTeamService,
                relayTeamMemberMapper,
                checkinRecordMapper,
                competitionResultService,
                sysUserService
        );
        ReflectionTestUtils.setField(service, "baseMapper", scheduleEntryMapper);

        lenient().when(scheduleService.getById(SCHEDULE_ID)).thenReturn(schedule());
        lenient().when(eventService.getById(EVENT_ID)).thenReturn(event());
        lenient().when(sportItemService.getById(SPORT_ITEM_ID)).thenReturn(sportItem());
        lenient().when(competitionResultService.list(anyWrapper())).thenReturn(List.of());
        lenient().when(checkinRecordMapper.selectOne(anyWrapper())).thenReturn(null);
    }

    @Test
    void returnsOnlyTheCurrentStudentsApprovedPersonalEntries() {
        Registration ownRegistration = registration(101L, USER_ID);
        Registration otherRegistration = registration(102L, OTHER_USER_ID);
        ScheduleEntry ownEntry = personalEntry(501L, 101L);
        ScheduleEntry otherEntry = personalEntry(502L, 102L);

        when(registrationService.list(anyWrapper())).thenReturn(List.of(ownRegistration));
        when(relayTeamMemberMapper.selectList(anyWrapper())).thenReturn(List.of());
        when(scheduleEntryMapper.selectList(anyWrapper()))
                .thenReturn(List.of(ownEntry), List.of(ownEntry, otherEntry));
        when(registrationService.getById(101L)).thenReturn(ownRegistration);
        when(registrationService.getById(102L)).thenReturn(otherRegistration);
        when(sysUserService.getById(USER_ID)).thenReturn(user(USER_ID, "当前学生"));
        when(sysUserService.getById(OTHER_USER_ID)).thenReturn(user(OTHER_USER_ID, "其他学生"));

        List<ScheduleEntryVO> result = service.listMyEntries(USER_ID);

        assertEquals(1, result.size());
        assertEquals(501L, result.get(0).getId());
        assertEquals("CK00000501", result.get(0).getCheckinCode());
        assertEquals("当前学生", result.get(0).getParticipantName());
        assertEquals("校运会", result.get(0).getEventName());
        assertEquals("男子100米", result.get(0).getSportItemName());
        assertEquals("PENDING", result.get(0).getCheckinStatus());

        ArgumentCaptor<Wrapper<Registration>> registrationQuery = wrapperCaptor();
        verify(registrationService).list(registrationQuery.capture());
        LambdaQueryWrapper<Registration> personalFilter =
                (LambdaQueryWrapper<Registration>) registrationQuery.getValue();
        assertTrue(personalFilter.getSqlSegment().contains("user_id"));
        assertTrue(personalFilter.getSqlSegment().contains("status"));
        assertTrue(personalFilter.getParamNameValuePairs().containsValue(USER_ID));
        assertTrue(personalFilter.getParamNameValuePairs().containsValue(2));
    }

    @Test
    void returnsRelayScheduleOnlyWhenTheCurrentStudentBelongsToAnApprovedTeam() {
        RelayTeam ownTeam = relayTeam(77L, "信息学院接力队");
        RelayTeam otherTeam = relayTeam(78L, "体育学院接力队");
        RelayTeamMember ownMember = teamMember(77L, USER_ID);
        RelayTeamMember otherMember = teamMember(78L, OTHER_USER_ID);
        ScheduleEntry ownEntry = relayEntry(601L, 77L);
        ScheduleEntry otherEntry = relayEntry(602L, 78L);

        when(registrationService.list(anyWrapper())).thenReturn(List.of());
        when(relayTeamMemberMapper.selectList(anyWrapper()))
                .thenReturn(List.of(ownMember), List.of(ownMember), List.of(otherMember));
        when(relayTeamService.list(anyWrapper())).thenReturn(List.of(ownTeam));
        when(scheduleEntryMapper.selectList(anyWrapper()))
                .thenReturn(List.of(ownEntry), List.of(ownEntry, otherEntry));
        when(relayTeamService.getById(77L)).thenReturn(ownTeam);
        when(relayTeamService.getById(78L)).thenReturn(otherTeam);
        when(sysUserService.getById(USER_ID)).thenReturn(user(USER_ID, "当前学生"));
        when(sysUserService.getById(OTHER_USER_ID)).thenReturn(user(OTHER_USER_ID, "其他学生"));

        List<ScheduleEntryVO> result = service.listMyEntries(USER_ID);

        assertEquals(1, result.size());
        assertEquals(601L, result.get(0).getId());
        assertEquals("CK00000601", result.get(0).getCheckinCode());
        assertEquals("信息学院接力队", result.get(0).getTeamName());

        ArgumentCaptor<Wrapper<RelayTeam>> teamQuery = wrapperCaptor();
        verify(relayTeamService).list(teamQuery.capture());
        LambdaQueryWrapper<RelayTeam> approvedTeamFilter =
                (LambdaQueryWrapper<RelayTeam>) teamQuery.getValue();
        assertTrue(approvedTeamFilter.getSqlSegment().contains("status"));
        assertTrue(approvedTeamFilter.getParamNameValuePairs().containsValue(2));

        ArgumentCaptor<Wrapper<RelayTeamMember>> membershipQuery = wrapperCaptor();
        verify(relayTeamMemberMapper, times(3)).selectList(membershipQuery.capture());
        LambdaQueryWrapper<RelayTeamMember> personalMembershipFilter =
                (LambdaQueryWrapper<RelayTeamMember>) membershipQuery.getAllValues().get(0);
        assertTrue(personalMembershipFilter.getSqlSegment().contains("user_id"));
        assertTrue(personalMembershipFilter.getParamNameValuePairs().containsValue(USER_ID));
    }

    @Test
    void returnsNoEntriesWhenUserHasNoApprovedRegistrationOrTeamMembership() {
        when(registrationService.list(anyWrapper())).thenReturn(List.of());
        when(relayTeamMemberMapper.selectList(anyWrapper())).thenReturn(List.of());

        List<ScheduleEntryVO> result = service.listMyEntries(USER_ID);

        assertTrue(result.isEmpty());
        verify(scheduleEntryMapper, times(0)).selectList(anyWrapper());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> org.mockito.ArgumentCaptor<Wrapper<T>> wrapperCaptor() {
        return (org.mockito.ArgumentCaptor) ArgumentCaptor.forClass(Wrapper.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Wrapper<T> anyWrapper() {
        return (Wrapper) any(Wrapper.class);
    }

    private static Schedule schedule() {
        Schedule value = new Schedule();
        value.setId(SCHEDULE_ID);
        value.setEventId(EVENT_ID);
        value.setSportItemId(SPORT_ITEM_ID);
        value.setScheduleName("男子100米预赛");
        value.setMatchDate(LocalDate.of(2026, 10, 18));
        value.setStartTime(LocalTime.of(9, 0));
        value.setLocation("田径场");
        value.setStatus(1);
        return value;
    }

    private static Event event() {
        Event value = new Event();
        value.setId(EVENT_ID);
        value.setName("校运会");
        return value;
    }

    private static SportItem sportItem() {
        SportItem value = new SportItem();
        value.setId(SPORT_ITEM_ID);
        value.setName("男子100米");
        value.setStatus(1);
        return value;
    }

    private static Registration registration(Long id, Long userId) {
        Registration value = new Registration();
        value.setId(id);
        value.setUserId(userId);
        value.setEventId(EVENT_ID);
        value.setSportItemId(SPORT_ITEM_ID);
        value.setStatus(2);
        return value;
    }

    private static ScheduleEntry personalEntry(Long id, Long registrationId) {
        ScheduleEntry value = new ScheduleEntry();
        value.setId(id);
        value.setScheduleId(SCHEDULE_ID);
        value.setRegistrationId(registrationId);
        value.setGroupNo("A");
        value.setLaneNo(1);
        value.setStartOrder(1);
        value.setQualificationStatus("ENTERED");
        return value;
    }

    private static ScheduleEntry relayEntry(Long id, Long teamId) {
        ScheduleEntry value = new ScheduleEntry();
        value.setId(id);
        value.setScheduleId(SCHEDULE_ID);
        value.setRelayTeamId(teamId);
        value.setGroupNo("A");
        value.setLaneNo(1);
        value.setStartOrder(1);
        value.setQualificationStatus("ENTERED");
        return value;
    }

    private static RelayTeam relayTeam(Long id, String name) {
        RelayTeam value = new RelayTeam();
        value.setId(id);
        value.setEventId(EVENT_ID);
        value.setSportItemId(SPORT_ITEM_ID);
        value.setTeamName(name);
        value.setStatus(2);
        return value;
    }

    private static RelayTeamMember teamMember(Long teamId, Long userId) {
        RelayTeamMember value = new RelayTeamMember();
        value.setTeamId(teamId);
        value.setUserId(userId);
        value.setLegOrder(1);
        value.setReserve(0);
        return value;
    }

    private static SysUser user(Long id, String realName) {
        SysUser value = new SysUser();
        value.setId(id);
        value.setUsername("student" + id);
        value.setRealName(realName);
        value.setStudentNo("S" + id);
        value.setCollege("测试学院");
        value.setClassName("测试班");
        return value;
    }
}
