package com.sports.modules.result.controller;

import com.sports.common.result.PageResult;
import com.sports.common.result.Result;
import com.sports.modules.result.entity.CompetitionResult;
import com.sports.modules.result.service.CompetitionResultService;
import com.sports.modules.result.vo.CompetitionResultVO;
import com.sports.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 比赛成绩 Controller
 *
 * @author sports
 */
@Tag(name = "成绩管理", description = "成绩的增删改查")
@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class CompetitionResultController {

    private final CompetitionResultService competitionResultService;

    @Operation(summary = "分页查询成绩")
    @GetMapping
    public Result<PageResult<CompetitionResultVO>> pageResults(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long sportItemId,
            @RequestParam(required = false) Long userId) {
        return Result.success(PageResult.of(competitionResultService.pageResultsVO(current, size, eventId, sportItemId, userId)));
    }

    @Operation(summary = "获取成绩排名")
    @GetMapping("/ranking")
    public Result<List<CompetitionResult>> getRanking(@RequestParam Long eventId, @RequestParam Long sportItemId) {
        return Result.success(competitionResultService.getRankingByEventAndItem(eventId, sportItemId));
    }

    @Operation(summary = "获取我的成绩")
    @GetMapping("/my")
    public Result<List<CompetitionResultVO>> getMyResults() {
        Long userId = SecurityUtils.getUserId();
        return Result.success(competitionResultService.getMyResultsVO(userId));
    }

    @Operation(summary = "录入成绩")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> saveResult(@Valid @RequestBody CompetitionResult result) {
        competitionResultService.save(result);
        return Result.success();
    }

    @Operation(summary = "批量录入成绩")
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> batchSaveResults(@RequestBody List<CompetitionResult> results) {
        competitionResultService.batchSaveResults(results);
        return Result.success();
    }

    @Operation(summary = "更新成绩")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> updateResult(@PathVariable Long id, @Valid @RequestBody CompetitionResult result) {
        result.setId(id);
        competitionResultService.updateById(result);
        return Result.success();
    }

    @Operation(summary = "删除成绩")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteResult(@PathVariable Long id) {
        competitionResultService.removeById(id);
        return Result.success();
    }
}