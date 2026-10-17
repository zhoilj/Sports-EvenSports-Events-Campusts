package com.sports.modules.registration.controller;

import com.sports.common.result.PageResult;
import com.sports.common.result.Result;
import com.sports.modules.registration.dto.RegistrationDTO;
import com.sports.modules.registration.service.RegistrationService;
import com.sports.modules.registration.vo.RegistrationVO;
import com.sports.modules.registration.vo.RegistrationMemberVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报名 Controller
 *
 * @author sports
 */
@Tag(name = "报名管理", description = "报名的增删改查")
@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @Operation(summary = "学生报名")
    @PostMapping
    public Result<Long> register(@Valid @RequestBody RegistrationDTO dto) {
        Long id = registrationService.register(dto);
        return Result.success("报名成功", id);
    }

    @Operation(summary = "取消报名")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelRegistration(@PathVariable Long id) {
        registrationService.cancelRegistration(id);
        return Result.success();
    }

    @Operation(summary = "审核报名")
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> reviewRegistration(@PathVariable Long id,
                                           @RequestParam String status,
                                           @RequestParam(required = false) String remark) {
        registrationService.reviewRegistration(id, status, remark);
        return Result.success();
    }

    @Operation(summary = "审核通过")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> approveRegistration(@PathVariable Long id) {
        registrationService.reviewRegistration(id, "APPROVED", null);
        return Result.success();
    }

    @Operation(summary = "审核拒绝")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> rejectRegistration(@PathVariable Long id,
                                           @RequestParam(required = false) String reason) {
        registrationService.reviewRegistration(id, "REJECTED", reason);
        return Result.success();
    }

    @Operation(summary = "分页查询报名列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<PageResult<RegistrationVO>> pageRegistrations(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long sportItemId,
            @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(registrationService.pageRegistrations(current, size, eventId, sportItemId, status)));
    }

    @Operation(summary = "获取我的报名列表")
    @GetMapping("/my")
    public Result<PageResult<RegistrationVO>> getMyRegistrations(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(registrationService.getMyRegistrations(current, size, status)));
    }

    @Operation(summary = "查询同学院可选接力队员")
    @GetMapping("/eligible-athletes")
    public Result<List<RegistrationMemberVO>> listEligibleAthletes(
            @RequestParam(required = false) String keyword) {
        return Result.success(registrationService.listEligibleAthletes(keyword));
    }

    @Operation(summary = "检查是否已报名")
    @GetMapping("/check")
    public Result<Boolean> checkRegistration(@RequestParam Long eventId, @RequestParam Long sportItemId) {
        Long userId = com.sports.security.util.SecurityUtils.getUserId();
        return Result.success(registrationService.hasRegistered(eventId, sportItemId, userId));
    }
}