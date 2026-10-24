package com.example.config;

import com.example.common.Result;
import com.example.common.ResultCode;
import com.example.module.user.service.RbacService;
import com.example.security.JwtAuthenticationFilter;
import com.example.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 配置（见 docs/技术方案.md §3.2 / §7）
 *
 * <p>无状态会话 + JWT 过滤器；方法级鉴权由 {@code @EnableMethodSecurity} 开启， 业务接口统一用
 * {@code @PreAuthorize("hasAuthority('<权限码>')")}，权限码来源见 sys_permission。
 *
 * <p>白名单：登录接口、健康检查、错误转发。其余接口一律要求已认证。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    private final RbacService rbacService;

    private final ObjectMapper objectMapper;

    /** BCrypt 密码编码器（sys_user.password 以 BCrypt 存储，见 docs/init.sql） */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 跨域配置（与 WebConfig 的开发期放行口径一致）
     *
     * <p>Security 过滤器链在 DispatcherServlet 之前执行，必须在安全层登记 CORS， 否则预检请求会在 MVC 配置生效前被认证拦截。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtil, rbacService);
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                                        .permitAll()
                                        .requestMatchers(
                                                "/api/v1/auth/login", "/actuator/health", "/error")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        handling ->
                                handling.authenticationEntryPoint(
                                                (request, response, ex) ->
                                                        writeError(
                                                                response,
                                                                ResultCode.UNAUTHORIZED,
                                                                "未登录或登录已过期"))
                                        .accessDeniedHandler(
                                                (request, response, ex) ->
                                                        writeError(
                                                                response,
                                                                ResultCode.FORBIDDEN,
                                                                "无权限访问")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 过滤器链内抛出的认证/授权异常不经过 GlobalExceptionHandler，这里直接写统一响应体 */
    private void writeError(HttpServletResponse response, ResultCode code, String message)
            throws IOException {
        response.setStatus(
                code == ResultCode.UNAUTHORIZED
                        ? HttpServletResponse.SC_UNAUTHORIZED
                        : HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Result.fail(code, message));
    }
}
