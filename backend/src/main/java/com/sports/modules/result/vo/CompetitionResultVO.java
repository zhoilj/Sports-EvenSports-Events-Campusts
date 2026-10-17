package com.sports.modules.result.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 比赛成绩 VO
 *
 * @author sports
 */
@Data
public class CompetitionResultVO {
    
    private Long id;
    
    /**
     * 赛事ID
     */
    private Long eventId;
    
    /**
     * 赛事名称
     */
    private String eventName;
    
    /**
     * 运动项目ID
     */
    private Long sportItemId;
    
    /**
     * 运动项目名称
     */
    private String sportItemName;
    
    /**
     * 用户ID
     */
    private Long userId;

    private Long registrationId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 学号
     */
    private String studentNo;
    
    /**
     * 学院
     */
    private String college;
    
    /**
     * 成绩
     */
    private BigDecimal score;
    
    /**
     * 成绩单位
     */
    private String unit;

    private String scoreText;
    
    /**
     * 名次
     */
    private Integer ranking;
    
    /**
     * 积分
     */
    private Integer points;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 记录人ID
     */
    private Long recorderId;
    
    /**
     * 记录人姓名
     */
    private String recorderName;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}