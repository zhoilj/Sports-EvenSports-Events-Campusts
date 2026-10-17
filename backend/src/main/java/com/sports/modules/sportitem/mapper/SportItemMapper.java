package com.sports.modules.sportitem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sports.modules.sportitem.entity.SportItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 运动项目 Mapper
 *
 * @author sports
 */
@Mapper
public interface SportItemMapper extends BaseMapper<SportItem> {

    /**
     * 原子预占一个报名名额，避免并发报名超过项目上限。
     * max_participants 为 NULL 或 0 时表示不限人数。
     */
    @Update("""
            UPDATE sport_item
            SET current_participants = COALESCE(current_participants, 0) + 1
            WHERE id = #{id}
              AND status = 1
              AND is_deleted = 0
              AND (max_participants IS NULL OR max_participants <= 0
                   OR COALESCE(current_participants, 0) < max_participants)
            """)
    int reserveParticipantSlot(@Param("id") Long id);

    /**
     * 释放一个已占用名额，不允许计数降至负数。
     */
    @Update("""
            UPDATE sport_item
            SET current_participants = GREATEST(COALESCE(current_participants, 0) - 1, 0)
            WHERE id = #{id} AND is_deleted = 0
            """)
    int releaseParticipantSlot(@Param("id") Long id);
}