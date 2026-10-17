package com.sports.modules.schedule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sports.modules.schedule.entity.CheckinRecord;
import com.sports.modules.schedule.vo.ScheduleEntryVO;

import java.util.List;

public interface CheckinRecordService extends IService<CheckinRecord> {
    List<ScheduleEntryVO> listBySchedule(Long scheduleId);
    void updateStatus(Long scheduleEntryId, String status, String remark);
}
