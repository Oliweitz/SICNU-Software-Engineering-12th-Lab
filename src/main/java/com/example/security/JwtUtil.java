package com.example.security;

import com.example.common.BizException;
import com.example.common.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 工具：签发与解析令牌（配置项见 application.yml 的 jwt.* 节）
 *
 * <p>载荷只放身份信息（id / username / realName）：角色与权限码由 {@code RbacService} 每请求现查， 授权调整即时生效，不会因旧令牌携带旧权限而越权。
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-hours}")
    private long expireHours;

    /** 签发令牌（HS256，密钥由 jwt.secret 派生，长度须 ≥ 32 字节） */
    public String generateToken(LoginUser user) {
        SecretKey key = signKey();
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("realName", user.getRealName())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireHours * 3600_000L))
                .signWith(key)
                .compact();
    }

    /** 解析并校验令牌；签名无效 / 过期 / 格式错误统一抛 UNAUTHORIZED */
    public LoginUser parseToken(String token) {
        try {
            Claims claims =
                    Jwts.parser()
                            .verifyWith(signKey())
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
            return LoginUser.builder()
                    .id(Long.valueOf(claims.getSubject()))
                    .username(claims.get("username", String.class))
                    .realName(claims.get("realName", String.class))
                    .build();
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException(ResultCode.UNAUTHORIZED, "令牌无效或已过期");
        }
    }

    private SecretKey signKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
