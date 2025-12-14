package com.sports.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求 DTO
 *
 * @author sports
 */
@Data
public class RegisterDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度为4-20个字符")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度为6-20个字符")
    private String password;

    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /**
     * 真实姓名
     */
    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名最多50个字符")
    private String realName;

    /**
     * 学号
     */
    @Size(max = 20, message = "学号最多20个字符")
    private String studentNo;

    /**
     * 学院
     */
    @Size(max = 100, message = "学院名称最多100个字符")
    private String college;

    /**
     * 班级
     */
    @Size(max = 50, message = "班级名称最多50个字符")
    private String className;

    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 性别
     */
    @Pattern(regexp = "^[MF]$", message = "性别只能是M或F")
    private String gender;
}