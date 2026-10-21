package com.sports.modules.schedule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sports.modules.schedule.dto.AdvanceEntriesDTO;
import com.sports.modules.schedule.dto.ScheduleEntryPositionDTO;
import com.sports.modules.schedule.entity.ScheduleEntry;
import com.sports.modules.schedule.vo.ScheduleConflictVO;
import com.sports.modules.schedule.vo.ScheduleEntryVO;

import java.util.List;

public interface ScheduleEntryService extends IService<ScheduleEntry> {
    int generateEntries(Long scheduleId, Integer groupSize, Integer laneCount);
    int updateEntryPositions(Long scheduleId, List<ScheduleEntryPositionDTO> positions);
    List<ScheduleEntryVO> listEntryDetails(Long scheduleId);
    List<ScheduleEntryVO> listMyEntries(Long userId);
    List<ScheduleConflictVO> checkTimeConflicts(Long scheduleId);
    int advanceEntries(AdvanceEntriesDTO dto);
}
