package com.sports.modules.result.controller;

import com.sports.common.result.Result;
import com.sports.modules.result.dto.ScoringRuleDTO;
import com.sports.modules.result.entity.ScoringRule;
import com.sports.modules.result.service.ScoringRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scoring-rules")
@RequiredArgsConstructor
public class ScoringRuleController {
    private final ScoringRuleService scoringRuleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<ScoringRule>> getRules(@RequestParam Long eventId) {
        return Result.success(scoringRuleService.getRules(eventId));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> replaceRules(@RequestParam Long eventId,
                                     @Valid @RequestBody List<ScoringRuleDTO> rules) {
        scoringRuleService.replaceRules(eventId, rules);
        return Result.success();
    }
}
