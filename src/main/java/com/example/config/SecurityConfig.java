package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置（见 docs/技术方案.md 第 3.2 节：Spring Security + JWT）
 *
 * <p>骨架阶段基线：无状态会话 + 全部放行，保证前后端联调不被默认登录页拦截。
 *
 * <p>TODO 迭代 1：接入 {@code JwtAuthenticationFilter}；开放 {@code /api/v1/auth/**}
 * 与公共资源，其余接口按角色鉴权（@PreAuthorize）。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** BCrypt 密码编码器（sys_user.password 以 BCrypt 存储，见 docs/init.sql） */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // TODO 迭代 1：按类注释收紧鉴权规则
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
