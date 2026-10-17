package com.sports.modules.schedule.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class ScheduleEntryVO {
    private Long id;
    private Long scheduleId;
    private String checkinCode;
    private String eventName;
    private String sportItemName;
    private String competitionGroup;
    private String scheduleName;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate matchDate;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;
    private String location;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkedAt;
    private String checkinRemark;
    private Long registrationId;
    private Long relayTeamId;
    private String teamName;
    private String participantName;
    private String studentNo;
    private String college;
    private String className;
    private String memberNames;
    private String groupNo;
    private Integer laneNo;
    private Integer startOrder;
    private String qualificationStatus;
    private String checkinStatus;
    private BigDecimal score;
    private String scoreText;
    private String resultStatus;
    private Integer ranking;
    private Integer reviewStatus;
    private Boolean published;
}
