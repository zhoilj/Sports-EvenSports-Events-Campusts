package com.sports.modules.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

import java.time.LocalDate;

/**
 * 赛事 DTO
 *
 * @author sports
 */
@Data
public class EventDTO {

    /**
     * 赛事名称
     */
    @NotBlank(message = "赛事名称不能为空")
    @Size(max = 100, message = "赛事名称最多100个字符")
    private String name;

    /**
     * 赛事描述
     */
    @Size(max = 2000, message = "赛事描述最多2000个字符")
    private String description;

    /**
     * 赛事类型
     */
    @NotBlank(message = "赛事类型不能为空")
    private String type;

    /**
     * 开始日期
     */
    @NotNull(message = "开始日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @NotNull(message = "结束日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 报名开始日期
     */
    @NotNull(message = "报名开始日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate regStartDate;

    /**
     * 报名截止日期
     */
    @NotNull(message = "报名截止日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate regEndDate;

    /**
     * 举办地点
     */
    @Size(max = 200, message = "举办地点最多200个字符")
    private String location;

    /**
     * 主办单位
     */
    @Size(max = 100, message = "主办单位最多100个字符")
    private String organizer;

    /**
     * 联系电话
     */
    @Size(max = 20, message = "联系电话最多20个字符")
    private String contactPhone;

    /**
     * 封面图片URL
     */
    private String coverImage;

    /**
     * 每名运动员可报名的项目数；0 表示不限制，未填写时默认3
     */
    @Min(value = 0, message = "每名运动员限报项目数不能小于0")
    @Max(value = 20, message = "每名运动员限报项目数不能超过20")
    private Integer maxProjectsPerAthlete;

    /**
     * 状态 (1-草稿 2-报名中 3-进行中 4-已结束)
     */
    private Integer status;
}