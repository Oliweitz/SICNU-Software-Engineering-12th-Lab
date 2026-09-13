package com.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 认证过滤器（见 docs/技术方案.md 第 7 节：Authorization: Bearer &lt;token&gt;）
 *
 * <p>TODO 迭代 1 实现并在 {@code SecurityConfig} 注册：解析请求头令牌 → 填充 {@link UserContext} → 构造 Authentication
 * 交给 Security 上下文。
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // TODO 迭代 1：解析 Authorization 头、校验令牌、填充 UserContext/SecurityContext
        filterChain.doFilter(request, response);
    }
}
