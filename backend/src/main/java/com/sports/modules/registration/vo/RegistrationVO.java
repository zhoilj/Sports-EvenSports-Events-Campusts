package com.sports.modules.registration.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private String registrationType;
    private String teamName;
    private String collegeSnapshot;
    private String classSnapshot;
    private String phone;
    private String className;
    private List<RegistrationMemberVO> members = new ArrayList<>();
    private Long reviewerId;
    private String reviewerName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;
    private String reviewRemark;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}