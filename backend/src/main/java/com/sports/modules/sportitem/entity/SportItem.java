package com.sports.modules.sportitem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sports.common.entity.BaseEntity;
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
     * 是否为接力项目（0-个人项目 1-接力项目）。
     */
    @TableField("is_relay")
    private Integer isRelay;

    /**
     * 接力队伍必须登记的正式队员人数。
     */
    @TableField("relay_member_count")
    private Integer relayMemberCount;

    /**
     * 接力队伍允许登记的替补人数。
     */
    @TableField("relay_reserve_count")
    private Integer relayReserveCount;

    /** 1 for any team-based event, including relays and collective/fun events. */
    @TableField("is_team_event")
    private Integer isTeamEvent;

    /** Required members for non-relay team events. */
    @TableField("team_member_count")
    private Integer teamMemberCount;

    /** Optional reserve members for non-relay team events. */
    @TableField("team_reserve_count")
    private Integer teamReserveCount;

    /** Maximum active teams allowed per college for this event item. */
    @TableField("max_teams_per_college")
    private Integer maxTeamsPerCollege;

    /**
     * 性别限制（0-不限 1-仅男 2-仅女）
     */
    @TableField("gender_limit")
    private Integer genderLimit;

    /** 甲组/乙组/教工组/不分组：A/B/STAFF/OPEN。 */
    private String division;

    /** MALE/FEMALE/MIXED/OPEN. */
    @TableField("gender_category")
    private String genderCategory;

    /** FINAL_ONLY/PRELIMINARY_FINAL/FLEXIBLE. */
    @TableField("round_policy")
    private String roundPolicy;

    /** Official scoring multiplier, e.g. 2 for relay and group events. */
    @TableField("scoring_multiplier")
    private Integer scoringMultiplier;

    /** Optional participation points for an eligible checked-in result. */
    @TableField("participation_points")
    private Integer participationPoints;

    /** Base points for a record-breaking result before multiplier. */
    @TableField("record_points")
    private Integer recordPoints;

    /**
     * 最大报名人数
     */
    @TableField("max_participants")
    private Integer maxParticipants;

    /**
     * 当前报名人数
     */
    @TableField("current_participants")
    private Integer currentParticipants;

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
     * Result ordering rule: ASC for lower-is-better (e.g. time), DESC for higher-is-better.
     */
    @TableField("sort_rule")
    private String sortRule;

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