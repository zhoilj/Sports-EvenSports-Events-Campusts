package com.sports.modules.registration.vo;

import lombok.Data;

@Data
public class RelayTeamMemberVO {
    private Long userId;
    private String realName;
    private String username;
    private String studentNo;
    private String college;
    private String className;
    private Integer legOrder;
    private Boolean reserve;
}
