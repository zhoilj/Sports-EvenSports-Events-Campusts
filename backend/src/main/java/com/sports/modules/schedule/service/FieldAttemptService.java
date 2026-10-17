package com.sports.modules.schedule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sports.modules.schedule.dto.FieldAttemptDTO;
import com.sports.modules.schedule.entity.FieldAttempt;

import java.util.List;

public interface FieldAttemptService extends IService<FieldAttempt> {
    List<FieldAttempt> listBySchedule(Long scheduleId);
    void saveAttempts(List<FieldAttemptDTO> attempts);
    int finalizeResults(Long scheduleId);
}
