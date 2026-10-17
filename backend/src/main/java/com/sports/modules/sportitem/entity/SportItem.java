package com.sports.modules.sportitem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sports.common.entity.BaseEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 运动项目实体
 *
 * @author sports
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sport_item")
public class SportItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属赛事ID
     */
    @TableField("event_id")
    private Long eventId;

    /**
     * 项目名称
     */
    @TableField("item_name")
    private String name;

    /**
     * 项目类型（TRACK-径赛 FIELD-田赛 BALL-球类 SWIMMING-游泳 OTHER-其他）
     */
    @TableField("item_type")
    private String type;

    /**
     * 性别限制（0-不限 1-仅男 2-仅女）
     */
    @TableField("gender_limit")
    private Integer genderLimit;

    /**
     * 最大报名人数
     */
    @TableField("max_participants")
    private Integer maxParticipants;

    /**
     * 当前有效报名数；接力项目按队伍计数
     */
    @TableField("current_participants")
    private Integer currentParticipants;

    /**
     * 参赛模式（INDIVIDUAL-个人项目 RELAY-接力项目）
     */
    @TableField("participation_mode")
    @Pattern(regexp = "INDIVIDUAL|RELAY", message = "参赛模式只能为 INDIVIDUAL 或 RELAY")
    private String participationMode;

    /**
     * 接力项目要求的队员人数；个人项目为空
     */
    @TableField("team_member_count")
    @Min(value = 2, message = "接力队员人数不能少于2人")
    @Max(value = 8, message = "接力队员人数不能超过8人")
    private Integer teamMemberCount;

    /**
     * 比赛规则说明
     */
    private String rules;

    /**
     * 成绩类型（TIME-计时 DISTANCE-计距离 SCORE-计分）
     */
    @TableField("result_type")
    private String scoreType;

    /**
     * 成绩单位
     */
    @TableField("result_unit")
    private String scoreUnit;

    /**
     * 状态（0-禁用 1-启用）
     */
    private Integer status;
}