package com.example.module.user;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.common.BizException;
import com.example.config.AuthProperties;
import com.example.module.user.service.impl.LoginAttemptServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 登录失败锁定单元测试 */
class LoginAttemptServiceImplTest {

    private LoginAttemptServiceImpl loginAttemptService;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties();
        properties.getLogin().setMaxFailures(3);
        properties.getLogin().setLockMinutes(15);
        loginAttemptService = new LoginAttemptServiceImpl(properties);
    }

    @Test
    @DisplayName("连续失败达到阈值后锁定，正确登录清除计数后可恢复")
    void lockAfterMaxFailures() {
        loginAttemptService.checkLocked("admin");
        loginAttemptService.recordFailure("admin");
        loginAttemptService.recordFailure("admin");
        loginAttemptService.recordFailure("admin");

        BizException e =
                assertThrows(BizException.class, () -> loginAttemptService.checkLocked("admin"));
        assertTrue(e.getMessage().contains("登录失败次数过多"));

        loginAttemptService.reset("admin");
        assertDoesNotThrow(() -> loginAttemptService.checkLocked("admin"));
    }

    @Test
    @DisplayName("不同账号的失败计数互不影响")
    void failuresIsolatedByUsername() {
        loginAttemptService.recordFailure("student");
        loginAttemptService.recordFailure("student");
        loginAttemptService.recordFailure("student");

        assertThrows(BizException.class, () -> loginAttemptService.checkLocked("student"));
        assertDoesNotThrow(() -> loginAttemptService.checkLocked("teacher"));
    }
}
