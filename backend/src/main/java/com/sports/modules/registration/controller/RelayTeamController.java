package com.sports.modules.registration.controller;

import com.sports.common.result.PageResult;
import com.sports.common.result.Result;
import com.sports.modules.registration.dto.RelayTeamCreateDTO;
import com.sports.modules.registration.service.RelayTeamService;
import com.sports.modules.registration.vo.RelayTeamMemberVO;
import com.sports.modules.registration.vo.RelayTeamVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/relay-teams")
@RequiredArgsConstructor
public class RelayTeamController {
    private final RelayTeamService relayTeamService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Long> createTeam(@Valid @RequestBody RelayTeamCreateDTO dto) {
        return Result.success("接力队伍已提交审核", relayTeamService.createTeam(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<PageResult<RelayTeamVO>> pageTeams(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long sportItemId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String college) {
        return Result.success(PageResult.of(
                relayTeamService.pageTeams(current, size, eventId, sportItemId, status, college)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<RelayTeamVO>> getMyTeams() {
        return Result.success(relayTeamService.getMyTeams());
    }

    @GetMapping("/eligible-members")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<RelayTeamMemberVO>> getEligibleMembers(
            @RequestParam(required = false) Long sportItemId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String keyword) {
        return Result.success(relayTeamService.getEligibleMembers(sportItemId, teamId, keyword));
    }

    @GetMapping("/{id}")
    public Result<RelayTeamVO> getTeamDetail(@PathVariable Long id) {
        return Result.success(relayTeamService.getTeamDetail(id));
    }

    @PutMapping("/{id}/members")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> replaceMembers(@PathVariable Long id, @RequestBody List<Long> memberUserIds) {
        relayTeamService.replaceMembers(id, memberUserIds);
        return Result.success();
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> reviewTeam(@PathVariable Long id,
                                   @RequestParam Integer status,
                                   @RequestParam(required = false) String remark) {
        relayTeamService.reviewTeam(id, status, remark);
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Void> cancelTeam(@PathVariable Long id) {
        relayTeamService.cancelTeam(id);
        return Result.success();
    }
}
