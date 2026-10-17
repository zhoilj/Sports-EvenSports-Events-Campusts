package com.sports.modules.registration.vo;

import lombok.Data;

@Data
public class RegistrationMemberVO {
    private Long userId;
    private String realName;
    private String studentNo;
    private String college;
    private String className;
    private Integer memberOrder;
}
