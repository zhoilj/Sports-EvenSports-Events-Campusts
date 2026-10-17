package com.sports.modules.schedule.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScheduleEntryVO {
    private Long id;
    private Long scheduleId;
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
