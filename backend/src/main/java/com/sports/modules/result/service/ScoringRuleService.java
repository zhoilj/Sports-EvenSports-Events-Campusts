package com.sports.modules.result.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sports.modules.result.dto.ScoringRuleDTO;
import com.sports.modules.result.entity.ScoringRule;

import java.util.List;

public interface ScoringRuleService extends IService<ScoringRule> {
    List<ScoringRule> getRules(Long eventId);
    void replaceRules(Long eventId, List<ScoringRuleDTO> rules);
}
