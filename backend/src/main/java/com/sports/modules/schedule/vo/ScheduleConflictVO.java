package com.sports.modules.schedule.vo;

import lombok.Data;

@Data
public class ScheduleConflictVO {
    private Long userId;
    private String participantName;
    private Long currentScheduleId;
    private String currentScheduleName;
    private String currentTime;
    private Long conflictingScheduleId;
    private String conflictingScheduleName;
    private String conflictingTime;
    private String location;
    private String message;
}
