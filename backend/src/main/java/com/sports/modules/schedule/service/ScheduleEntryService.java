package com.sports.modules.schedule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sports.modules.schedule.dto.CheckinUpdateDTO;
import com.sports.modules.schedule.dto.ScheduleEntryAssignmentDTO;
import com.sports.modules.schedule.entity.ScheduleEntry;
import com.sports.modules.schedule.vo.ScheduleEntryVO;

import java.util.List;

public interface ScheduleEntryService extends IService<ScheduleEntry> {
    List<ScheduleEntryVO> listByScheduleId(Long scheduleId);
    List<ScheduleEntryVO> listMyEntries();
    int autoAssign(Long scheduleId, Integer laneCount);
    void updateAssignment(Long id, ScheduleEntryAssignmentDTO dto);
    void checkIn(Long id, CheckinUpdateDTO dto);
    void removeEntry(Long id);
}
