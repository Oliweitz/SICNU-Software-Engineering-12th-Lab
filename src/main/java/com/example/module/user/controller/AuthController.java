package com.example.module.user.controller;

import com.example.common.Result;
import com.example.module.user.dto.CaptchaVO;
import com.example.module.user.dto.CodeLoginRequest;
import com.example.module.user.dto.LoginRequest;
import com.example.module.user.dto.LoginResponse;
import com.example.module.user.dto.RegisterRequest;
import com.example.module.user.dto.ResetPasswordRequest;
import com.example.module.user.dto.SendCodeRequest;
import com.example.module.user.dto.SendCodeResponse;
import com.example.module.user.service.AuthService;
import com.example.module.user.service.CaptchaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（统一前缀 /api/v1，见 docs/技术方案.md §7 与 docs/认证与授权设计.md）
 *
 * <ul>
 *   <li>GET /api/v1/auth/captcha —— 图形验证码
 *   <li>POST /api/v1/auth/login —— 账号密码登录
 *   <li>POST /api/v1/auth/login/code —— 手机号/邮箱验证码登录
 *   <li>POST /api/v1/auth/code/send —— 发送验证码（登录/注册/找回密码）
 *   <li>POST /api/v1/auth/register —— 注册
 *   <li>POST /api/v1/auth/password/reset —— 忘记密码重置
 * </ul>
 *
 * <p>统一身份认证（CAS）接口见 {@link CasController}。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final CaptchaService captchaService;

    /** 图形验证码（无需登录） */
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.ok(captchaService.generate());
    }

    /** 账号密码登录 */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    /** 手机号/邮箱验证码登录 */
    @PostMapping("/login/code")
    public Result<LoginResponse> loginByCode(@Valid @RequestBody CodeLoginRequest request) {
        return Result.ok(authService.loginByCode(request));
    }

    /** 发送验证码 */
    @PostMapping("/code/send")
    public Result<SendCodeResponse> sendCode(@Valid @RequestBody SendCodeRequest request) {
        return Result.ok(authService.sendCode(request));
    }

    /** 注册 */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.ok();
    }

    /** 忘记密码：验证码校验通过后重置 */
    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return Result.ok();
    }
}
