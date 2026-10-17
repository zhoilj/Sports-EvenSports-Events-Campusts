package com.sports.modules.registration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sports.modules.registration.dto.RegistrationDTO;
import com.sports.security.util.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;

/**
 * MySQL-backed API integration tests. Run with: mvn -Pmysql-integration verify
 * Tests run against an isolated MySQL database after V001 has been applied.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistrationMySqlIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtUtils jwtUtils;

    @Test
    void individualRegistrationCanCancelAndResubmitThroughApi() throws Exception {
        Fixture fixture = createFixture("INDIVIDUAL", null, 1);
        UserFixture student = fixture.students().get(0);
        RegistrationDTO dto = registration(fixture, null, null);

        long firstId = register(student, dto);
        assertEquals(1, participantCount(fixture.itemId()));
        assertEquals(0, memberCount(firstId));

        mockMvc.perform(post("/api/registrations/{id}/cancel", firstId)
                        .header("Authorization", bearer(student))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assertEquals(4, jdbcTemplate.queryForObject(
                "SELECT status FROM registration WHERE id = ?", Integer.class, firstId));
        assertEquals(0, participantCount(fixture.itemId()));

        long secondId = register(student, dto);
        assertNotEquals(firstId, secondId, "cancelled entry must not block a new registration");
        assertEquals(1, participantCount(fixture.itemId()));

        JsonNode page = myRegistrations(student);
        JsonNode records = page.path("data").path("records");
        assertTrue(records.isArray(), "GET /api/registrations/my must return data.records");
        assertTrue(findRecord(records, secondId).isPresent(),
                "the newly submitted registration must appear in the student's list");
        assertEquals("INDIVIDUAL", findRecord(records, secondId).orElseThrow()
                .path("registrationType").asText());
    }

    @Test
    void relayRosterIsReturnedByApiAndRejectionReleasesCapacity() throws Exception {
        Fixture fixture = createFixture("RELAY", 4, 4);
        UserFixture captain = fixture.students().get(0);
        UserFixture teacher = createUser("TEACHER", fixture.collegeName());
        RegistrationDTO dto = registration(fixture, "校运会接力联调队", fixture.students());

        long registrationId = register(captain, dto);
        assertEquals(1, participantCount(fixture.itemId()));
        assertEquals(4, memberCount(registrationId));

        JsonNode page = myRegistrations(captain);
        JsonNode records = page.path("data").path("records");
        JsonNode record = findRecord(records, registrationId).orElseThrow();
        assertEquals("RELAY", record.path("registrationType").asText());
        assertEquals("校运会接力联调队", record.path("teamName").asText());
        JsonNode members = record.path("members");
        assertEquals(4, members.size());
        for (int index = 0; index < fixture.students().size(); index++) {
            assertEquals(fixture.students().get(index).id(), members.get(index).path("userId").asLong());
            assertEquals(index + 1, members.get(index).path("memberOrder").asInt());
        }

        mockMvc.perform(post("/api/registrations/{id}/reject", registrationId)
                        .param("reason", "MySQL接口联调拒绝原因")
                        .header("Authorization", bearer(teacher)))
                .andExpect(status().isOk());

        assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT status FROM registration WHERE id = ?", Integer.class, registrationId));
        assertEquals("MySQL接口联调拒绝原因", jdbcTemplate.queryForObject(
                "SELECT reject_reason FROM registration WHERE id = ?", String.class, registrationId));
        assertEquals(0, participantCount(fixture.itemId()));
    }

    @Test
    void relayRegistrationFromDifferentCollegesIsRejectedWithoutWrites() throws Exception {
        Fixture fixture = createFixture("RELAY", 2, 1);
        UserFixture captain = fixture.students().get(0);
        UserFixture otherCollegeStudent = createUser("STUDENT", fixture.collegeName() + "-不同学院");
        RegistrationDTO dto = registration(fixture, "不应保存的接力队",
                List.of(captain, otherCollegeStudent));

        MvcResult result = mockMvc.perform(post("/api/registrations")
                        .header("Authorization", bearer(captain))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andReturn();

        assertTrue(result.getResponse().getStatus() < 500,
                "qualification failure should be handled as a business response: "
                        + result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertFalse(response.path("data").canConvertToLong(),
                "cross-college relay roster must not return a registration ID");
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registration WHERE event_id = ? AND sport_item_id = ? AND remark = ?",
                Integer.class, fixture.eventId(), fixture.itemId(), "cross-college-fixture"));
        assertEquals(0, participantCount(fixture.itemId()));
    }

    private Fixture createFixture(String mode, Integer teamMemberCount, int studentCount) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String collegeName = "集成测试学院-" + suffix;
        List<UserFixture> students = new ArrayList<>();
        for (int i = 0; i < studentCount; i++) {
            students.add(createUser("STUDENT", collegeName));
        }
        long eventId = insertAndGetId(
                "INSERT INTO event (event_name,event_type,description,start_date,end_date,reg_start_date,reg_end_date,location,organizer,contact_phone,status,is_deleted,create_by) "
                        + "VALUES (?,'TRACK_FIELD','MySQL registration integration',DATE_ADD(CURDATE(), INTERVAL 3 DAY),DATE_ADD(CURDATE(), INTERVAL 4 DAY),DATE_SUB(CURDATE(), INTERVAL 1 DAY),DATE_ADD(CURDATE(), INTERVAL 2 DAY),'CI田径场','集成测试','0000000000',2,0,1)",
                "CI校运会-" + suffix);
        long itemId = insertAndGetId(
                "INSERT INTO sport_item (event_id,item_name,item_type,gender_limit,max_participants,result_type,result_unit,sort_rule,rules,status,is_deleted,participation_mode,team_member_count,current_participants) "
                        + "VALUES (?,?,'TRACK',0,20,'TIME','秒','ASC','MySQL integration fixture',1,0,?,?,0)",
                eventId, "CI项目-" + suffix, mode, teamMemberCount);
        return new Fixture(eventId, itemId, collegeName, students);
    }

    private UserFixture createUser(String role, String collegeName) {
        jdbcTemplate.update("INSERT IGNORE INTO org_college (college_name,status) VALUES (?,1)", collegeName);
        Long collegeId = jdbcTemplate.queryForObject(
                "SELECT id FROM org_college WHERE college_name = ?", Long.class, collegeName);
        String className = "集成测试班";
        jdbcTemplate.update("INSERT IGNORE INTO org_class (college_id,class_name,status) VALUES (?,?,1)",
                collegeId, className);
        Long classId = jdbcTemplate.queryForObject(
                "SELECT id FROM org_class WHERE college_id = ? AND class_name = ?", Long.class, collegeId, className);

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String username = "it_" + role.toLowerCase() + "_" + suffix;
        String studentNo = "IT" + suffix;
        long id = insertAndGetId(
                "INSERT INTO sys_user (username,password,real_name,gender,role,student_no,department,class_name,status,is_deleted,college_id,class_id) "
                        + "VALUES (?,'not-used','集成测试运动员',1,?,?,?, ?,1,0,?,?)",
                username, role, studentNo, collegeName, className, collegeId, classId);
        return new UserFixture(id, username, role);
    }

    private RegistrationDTO registration(Fixture fixture, String teamName, List<UserFixture> members) {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setEventId(fixture.eventId());
        dto.setSportItemId(fixture.itemId());
        dto.setRemark("cross-college-fixture");
        if (teamName != null) {
            dto.setTeamName(teamName);
        }
        if (members != null) {
            dto.setMemberUserIds(members.stream().map(UserFixture::id).toList());
        }
        return dto;
    }

    private long register(UserFixture user, RegistrationDTO dto) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/registrations")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertTrue(response.path("data").canConvertToLong(),
                "registration API did not return an ID: " + result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return response.path("data").asLong();
    }

    private JsonNode myRegistrations(UserFixture user) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/registrations/my")
                        .param("current", "1")
                        .param("size", "50")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private java.util.Optional<JsonNode> findRecord(JsonNode records, long id) {
        if (!records.isArray()) {
            return java.util.Optional.empty();
        }
        for (JsonNode record : records) {
            if (record.path("id").asLong(-1) == id) {
                return java.util.Optional.of(record);
            }
        }
        return java.util.Optional.empty();
    }

    private String bearer(UserFixture user) {
        return "Bearer " + jwtUtils.generateToken(user.id(), user.username(), user.role());
    }

    private int participantCount(long itemId) {
        return jdbcTemplate.queryForObject(
                "SELECT current_participants FROM sport_item WHERE id = ?", Integer.class, itemId);
    }

    private int memberCount(long registrationId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registration_member WHERE registration_id = ?", Integer.class, registrationId);
    }

    private long insertAndGetId(String sql, Object... params) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int index = 0; index < params.length; index++) {
                statement.setObject(index + 1, params[index]);
            }
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        assertNotNull(key, "database did not return a generated ID");
        return key.longValue();
    }

    private record UserFixture(long id, String username, String role) {}
    private record Fixture(long eventId, long itemId, String collegeName, List<UserFixture> students) {}
}
