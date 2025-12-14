package com.sports.modules.registration.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报名 VO
 *
 * @author sports
 */
@Data
public class RegistrationVO {

    private Long id;
    private Long eventId;
    private String eventName;
    private Long sportItemId;
    private String sportItemName;
    private Long userId;
    private String username;
    private String realName;
    private String studentNo;
    private String college;
    private String status;
    private String statusName;
    private Long reviewerId;
    private String reviewerName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;
    private String reviewRemark;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}