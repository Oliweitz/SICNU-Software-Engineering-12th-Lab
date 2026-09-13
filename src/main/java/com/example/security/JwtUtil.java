package com.example.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 工具：签发与解析令牌（配置项见 application.yml 的 jwt.* 节）
 *
 * <p>TODO 迭代 1 实现：jjwt 依赖已引入（见 pom.xml）——
 *
 * <ul>
 *   <li>{@link #generateToken(LoginUser)}：按 secret / expireHours 签发 HS256 令牌
 *   <li>{@link #parseToken(String)}：校验签名与有效期，返回 {@link LoginUser}
 * </ul>
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-hours}")
    private long expireHours;

    /** TODO 迭代 1：签发令牌，载荷含 id / username / role / clazzId */
    public String generateToken(LoginUser user) {
        throw new UnsupportedOperationException("迭代 1 实现：JWT 签发");
    }

    /** TODO 迭代 1：解析并校验令牌；签名无效/过期时抛 BizException(UNAUTHORIZED) */
    public LoginUser parseToken(String token) {
        throw new UnsupportedOperationException("迭代 1 实现：JWT 解析");
    }
}
