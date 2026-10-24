package com.example.module.user.service;

import com.example.module.user.dto.LoginRequest;
import com.example.module.user.dto.LoginResponse;

/**
 * 用户业务（见 docs/技术方案.md 第 2.1 / 7 节）
 *
 * <p>登录接口归属定案：{@code POST /api/v1/auth/login} 由 user 模块实现（见 §5.1）， security 模块只提供 JwtUtil 与过滤器。
 */
public interface UserService {

    /**
     * 登录：BCrypt 校验密码 → 装配角色与权限码 → 签发 JWT
     *
     * @param request 登录名 + 明文密码
     * @return 令牌与身份、授权信息
     */
    LoginResponse login(LoginRequest request);
}
