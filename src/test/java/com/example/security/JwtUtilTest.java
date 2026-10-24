package com.example.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.common.BizException;
import com.example.common.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** {@link JwtUtil} 签发与解析单元测试 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(
                jwtUtil, "secret", "unit-test-secret-key-for-jwt-signing-2026");
        ReflectionTestUtils.setField(jwtUtil, "expireHours", 24L);
    }

    @Test
    @DisplayName("签发的令牌可解析回同一身份，且不携带角色权限")
    void generateAndParse() {
        LoginUser user = LoginUser.builder().id(42L).username("2022001").realName("张三").build();

        LoginUser parsed = jwtUtil.parseToken(jwtUtil.generateToken(user));

        assertEquals(42L, parsed.getId());
        assertEquals("2022001", parsed.getUsername());
        assertEquals("张三", parsed.getRealName());
        // 角色与权限由 RbacService 每请求现查，令牌不承载
        assertNull(parsed.getRoles());
        assertNull(parsed.getPermissions());
    }

    @Test
    @DisplayName("被篡改的令牌抛出 UNAUTHORIZED")
    void tamperedTokenRejected() {
        String token = jwtUtil.generateToken(LoginUser.builder().id(1L).username("u").build());
        String tampered = token.substring(0, token.length() - 2) + "xx";

        BizException e = assertThrows(BizException.class, () -> jwtUtil.parseToken(tampered));
        assertEquals(ResultCode.UNAUTHORIZED, e.getResultCode());
    }
}
