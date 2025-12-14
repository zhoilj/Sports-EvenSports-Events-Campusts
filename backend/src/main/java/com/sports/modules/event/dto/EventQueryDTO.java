package com.sports.modules.event.dto;

import lombok.Data;

/**
 * 赛事查询 DTO
 *
 * @author sports
 */
@Data
public class EventQueryDTO {

    /**
     * 当前页码
     */
    private Integer current = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;

    /**
     * 赛事名称（模糊查询）
     */
    private String name;

    /**
     * 赛事类型
     */
    private String type;

    /**
     * 状态 (1-草稿 2-报名中 3-进行中 4-已结束)
     */
    private Integer status;

    /**
     * 年份
     */
    private Integer year;
}