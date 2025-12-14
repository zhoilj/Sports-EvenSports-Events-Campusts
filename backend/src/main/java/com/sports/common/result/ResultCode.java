package com.sports.common.result;

import lombok.Getter;

/**
 * 响应码枚举
 *
 * @author sports
 */
@Getter
public enum ResultCode {

    // 成功
    SUCCESS(200, "操作成功"),

    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "数据冲突"),

    // 业务错误 5xx
    ERROR(500, "系统内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // 认证相关 1xxx
    TOKEN_INVALID(1001, "Token无效"),
    TOKEN_EXPIRED(1002, "Token已过期"),
    LOGIN_FAILED(1003, "用户名或密码错误"),
    ACCOUNT_DISABLED(1004, "账号已被禁用"),
    ACCOUNT_LOCKED(1005, "账号已被锁定"),

    // 用户相关 2xxx
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_ALREADY_EXISTS(2002, "用户已存在"),
    PASSWORD_ERROR(2003, "密码错误"),
    OLD_PASSWORD_ERROR(2004, "原密码错误"),
    PASSWORD_NOT_MATCH(2005, "两次密码不一致"),

    // 赛事相关 3xxx
    EVENT_NOT_FOUND(3001, "赛事不存在"),
    EVENT_NOT_PUBLISHED(3002, "赛事未发布"),
    EVENT_ENDED(3003, "赛事已结束"),
    EVENT_REGISTRATION_NOT_STARTED(3004, "报名未开始"),
    EVENT_REGISTRATION_ENDED(3005, "报名已结束"),

    // 报名相关 4xxx
    REGISTRATION_NOT_FOUND(4001, "报名记录不存在"),
    REGISTRATION_ALREADY_EXISTS(4002, "已报名该项目"),
    REGISTRATION_LIMIT_EXCEEDED(4003, "报名人数已满"),
    REGISTRATION_NOT_ALLOWED(4004, "不符合报名条件"),
    REGISTRATION_CANNOT_CANCEL(4005, "无法取消报名"),

    // 赛程相关 5xxx
    SCHEDULE_NOT_FOUND(5001, "赛程不存在"),
    SCHEDULE_CONFLICT(5002, "赛程时间冲突"),
    SCHEDULE_VENUE_CONFLICT(5003, "场地时间冲突"),

    // 成绩相关 6xxx
    RESULT_NOT_FOUND(6001, "成绩记录不存在"),
    RESULT_ALREADY_EXISTS(6002, "成绩已录入"),
    RESULT_CANNOT_MODIFY(6003, "成绩无法修改"),

    // 项目相关 7xxx
    SPORT_ITEM_NOT_FOUND(7001, "运动项目不存在"),
    SPORT_ITEM_DISABLED(7002, "运动项目已禁用"),

    // 文件相关 8xxx
    FILE_UPLOAD_ERROR(8001, "文件上传失败"),
    FILE_TYPE_NOT_ALLOWED(8002, "文件类型不允许"),
    FILE_SIZE_EXCEEDED(8003, "文件大小超限"),
    FILE_NOT_FOUND(8004, "文件不存在"),

    // 参数校验 9xxx
    PARAM_ERROR(9001, "参数校验失败"),
    PARAM_MISSING(9002, "缺少必要参数"),
    PARAM_TYPE_ERROR(9003, "参数类型错误");

    /**
     * 响应码
     */
    private final Integer code;

    /**
     * 响应消息
     */
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}