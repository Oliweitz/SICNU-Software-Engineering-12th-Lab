package com.example.module.user.controller;

import com.example.common.Result;
import com.example.module.user.dto.LoginRequest;
import com.example.module.user.dto.LoginResponse;
import com.example.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（统一前缀 /api/v1，见 docs/技术方案.md §7）
 *
 * <p>登录业务归 user 模块（技术方案 §5.1 归属定案），security 模块只提供 JWT 工具与过滤器。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** 登录：返回 JWT 与当前用户的角色、权限码 */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(userService.login(request));
    }
}
