package com.example.module.user.service;

import com.example.module.user.dto.CodeLoginRequest;
import com.example.module.user.dto.LoginRequest;
import com.example.module.user.dto.LoginResponse;
import com.example.module.user.dto.RegisterRequest;
import com.example.module.user.dto.ResetPasswordRequest;
import com.example.module.user.dto.SendCodeRequest;
import com.example.module.user.dto.SendCodeResponse;

/**
 * 认证业务（登录、注册、验证码、找回密码）
 *
 * <p>登录接口归属定案：{@code POST /api/v1/auth/**} 由 user 模块实现； security 模块只提供 JwtUtil、过滤器与
 * SecurityContext。
 */
public interface AuthService {

    /** 账号密码登录（图形验证码 + BCrypt 慢哈希比对 + 失败锁定） */
    LoginResponse login(LoginRequest request);

    /** 手机号/邮箱验证码登录 */
    LoginResponse loginByCode(CodeLoginRequest request);

    /** 发送登录/注册/找回密码验证码 */
    SendCodeResponse sendCode(SendCodeRequest request);

    /** 注册（默认 STUDENT 角色；手机号必须经短信验证码验证） */
    void register(RegisterRequest request);

    /** 忘记密码：验证码校验通过后重置密码 */
    void resetPassword(ResetPasswordRequest request);

    /**
     * 统一身份认证通过后，按本地学号/工号签发登录凭证
     *
     * <p>只允许在 CAS ticket 校验成功后调用，不能直接暴露为对外接口。
     */
    LoginResponse loginByCas(String username);
}
