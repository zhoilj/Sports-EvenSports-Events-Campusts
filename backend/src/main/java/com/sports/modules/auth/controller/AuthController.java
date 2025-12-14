package com.sports.modules.auth.controller;

import com.sports.common.result.Result;
import com.sports.modules.auth.dto.LoginDTO;
import com.sports.modules.auth.dto.RegisterDTO;
import com.sports.modules.auth.service.AuthService;
import com.sports.modules.auth.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证 Controller
 *
 * @author sports
 */
@Tag(name = "认证管理", description = "登录、注册、获取用户信息")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = authService.login(loginDTO);
        return Result.success(loginVO);
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterDTO registerDTO) {
        Long userId = authService.register(registerDTO);
        return Result.success("注册成功", userId);
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<LoginVO> getCurrentUser() {
        LoginVO loginVO = authService.getCurrentUser();
        return Result.success(loginVO);
    }
}