package com.sports.modules.registration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sports.modules.registration.entity.Registration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 报名 Mapper
 */
@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {

    /** 原子取消待审核报名，避免重复取消导致名额被多次释放。 */
    @Update("UPDATE registration SET status = 4 WHERE id = #{id} AND status = 1 AND is_deleted = 0")
    int cancelPending(@Param("id") Long id);

    /** 原子审核待审核报名，避免并发审核重复更新和重复释放名额。 */
    @Update("""
            UPDATE registration
            SET status = #{status}, audit_by = #{reviewerId}, audit_time = #{reviewTime}, reject_reason = #{remark}
            WHERE id = #{id} AND status = 1 AND is_deleted = 0
            """)
    int reviewPending(@Param("id") Long id,
                      @Param("status") Integer status,
                      @Param("reviewerId") Long reviewerId,
                      @Param("reviewTime") LocalDateTime reviewTime,
                      @Param("remark") String remark);
}
