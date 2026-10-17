package com.sports.modules.result.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.common.exception.BusinessException;
import com.sports.modules.event.service.EventService;
import com.sports.modules.result.dto.ScoringRuleDTO;
import com.sports.modules.result.entity.ScoringRule;
import com.sports.modules.result.mapper.ScoringRuleMapper;
import com.sports.modules.result.service.ScoringRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScoringRuleServiceImpl extends ServiceImpl<ScoringRuleMapper, ScoringRule> implements ScoringRuleService {
    private final EventService eventService;

    @Override
    public List<ScoringRule> getRules(Long eventId) {
        return this.list(new LambdaQueryWrapper<ScoringRule>()
                .eq(ScoringRule::getEventId, eventId)
                .orderByAsc(ScoringRule::getPlaceNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceRules(Long eventId, List<ScoringRuleDTO> rules) {
        if (eventService.getById(eventId) == null) {
            throw new BusinessException("赛事不存在");
        }
        if (rules == null || rules.isEmpty()) {
            throw new BusinessException("至少配置一条名次积分规则");
        }
        Set<Integer> places = new HashSet<>();
        for (ScoringRuleDTO rule : rules) {
            if (rule == null || rule.getPlaceNo() == null || rule.getPlaceNo() < 1
                    || rule.getPoints() == null || rule.getPoints() < 0) {
                throw new BusinessException("名次必须大于0，积分不能小于0");
            }
            if (!places.add(rule.getPlaceNo())) {
                throw new BusinessException("名次积分规则不能重复配置同一名次");
            }
        }
        this.remove(new LambdaQueryWrapper<ScoringRule>().eq(ScoringRule::getEventId, eventId));
        List<ScoringRule> entities = rules.stream().map(dto -> {
            ScoringRule entity = new ScoringRule();
            entity.setEventId(eventId);
            entity.setPlaceNo(dto.getPlaceNo());
            entity.setPoints(dto.getPoints());
            entity.setRuleName(dto.getRuleName());
            return entity;
        }).collect(Collectors.toList());
        this.saveBatch(entities);
    }
}
