package com.sports.modules.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sports.common.result.Result;
import com.sports.modules.system.dto.UserDTO;
import com.sports.modules.system.dto.UserQueryDTO;
import com.sports.modules.system.service.SysUserService;
import com.sports.modules.system.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理 Controller
 *
 * @author sports
 */
@Tag(name = "用户管理", description = "用户的增删改查")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService sysUserService;

    @Operation(summary = "分页查询用户")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<UserVO>> pageUsers(UserQueryDTO queryDTO) {
        return Result.success(sysUserService.pageUsers(queryDTO));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        return Result.success(sysUserService.getUserById(id));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> createUser(@Valid @RequestBody UserDTO userDTO) {
        return Result.success(sysUserService.createUser(userDTO));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
        sysUserService.updateUser(id, userDTO);
        return Result.success();
    }

    @Operation(summary = "重置用户密码")
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> resetPassword(@PathVariable Long id) {
        sysUserService.resetPassword(id);
        return Result.success();
    }

    @Operation(summary = "切换用户状态")
    @PostMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        sysUserService.toggleStatus(id);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteUser(@PathVariable Long id) {
        sysUserService.removeById(id);
        return Result.success();
    }
}
