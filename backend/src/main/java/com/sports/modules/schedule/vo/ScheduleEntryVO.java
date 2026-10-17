package com.sports.modules.schedule.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 编排和检录页面使用的参赛记录视图。
 */
@Data
public class ScheduleEntryVO {
    private Long id;
    private Long scheduleId;
    private Long registrationId;
    private Long relayTeamId;
    private String checkinCode;
    private String groupNo;
    private Integer laneNo;
    private Integer startOrder;
    private String qualificationStatus;

    private String participantType;
    private String participantName;
    private String studentNo;
    private String college;
    private String className;
    private String teamName;
    private List<String> memberNames;

    private Long eventId;
    private String eventName;
    private Long sportItemId;
    private String sportItemName;
    private String scheduleName;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate matchDate;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;
    private String location;

    private String checkinStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkedAt;
    private String checkinRemark;
}
