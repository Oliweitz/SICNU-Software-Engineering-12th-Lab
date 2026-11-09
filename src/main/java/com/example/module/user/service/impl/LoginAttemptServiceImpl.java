package com.example.module.user.service.impl;

import com.example.common.BizException;
import com.example.common.ResultCode;
import com.example.config.AuthProperties;
import com.example.module.user.service.LoginAttemptService;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 登录失败锁定实现（内存计数）
 *
 * <p>对「不存在的用户名」同样计数，避免通过锁定行为反推账号是否存在。 登录成功后清零；锁定到期后自动恢复。
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private final AuthProperties authProperties;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    @Override
    public void checkLocked(String username) {
        String key = key(username);
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return;
        }
        synchronized (attempt) {
            long now = System.currentTimeMillis();
            if (attempt.lockUntil > now) {
                long remainMinutes = Math.max(1, (attempt.lockUntil - now + 59_999) / 60_000);
                throw new BizException(
                        ResultCode.UNAUTHORIZED, "登录失败次数过多，请 " + remainMinutes + " 分钟后再试");
            }
            if (attempt.lockUntil > 0) {
                attempts.remove(key);
            }
        }
    }

    @Override
    public void recordFailure(String username) {
        String key = key(username);
        long now = System.currentTimeMillis();
        Attempt attempt = attempts.computeIfAbsent(key, k -> new Attempt());
        synchronized (attempt) {
            if (attempt.lockUntil > now) {
                return;
            }
            attempt.lastFailureAt = now;
            attempt.failures++;
            if (attempt.failures >= authProperties.getLogin().getMaxFailures()) {
                attempt.failures = 0;
                attempt.lockUntil = now + authProperties.getLogin().getLockMinutes() * 60_000L;
            }
        }
        cleanupExpired(now);
    }

    @Override
    public void reset(String username) {
        attempts.remove(key(username));
    }

    private String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private void cleanupExpired(long now) {
        long staleMillis = 30 * 60_000L;
        attempts.entrySet()
                .removeIf(
                        entry -> {
                            Attempt attempt = entry.getValue();
                            synchronized (attempt) {
                                return attempt.lockUntil <= now
                                        && now - attempt.lastFailureAt > staleMillis;
                            }
                        });
    }

    private static class Attempt {
        private int failures;
        private long lockUntil;
        private long lastFailureAt;
    }
}
