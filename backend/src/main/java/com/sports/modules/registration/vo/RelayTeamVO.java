package com.sports.modules.registration.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RelayTeamVO {
    private Long id;
    private Long eventId;
    private String eventName;
    private Long sportItemId;
    private String sportItemName;
    private Integer isRelay;
    private Integer isTeamEvent;
    private String teamName;
    private String college;
    private String className;
    private Long creatorUserId;
    private String creatorName;
    private Integer status;
    private String statusName;
    private Long reviewerId;
    private String reviewerName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;
    private String reviewRemark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private List<RelayTeamMemberVO> members;
}
