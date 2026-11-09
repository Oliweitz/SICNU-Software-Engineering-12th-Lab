package com.example.module.user.service.impl;

import com.example.common.BizException;
import com.example.common.ResultCode;
import com.example.config.AuthProperties;
import com.example.module.user.channel.EmailCodeSender;
import com.example.module.user.channel.SmsSender;
import com.example.module.user.enums.CodeChannel;
import com.example.module.user.enums.CodeScene;
import com.example.module.user.service.VerificationCodeService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 短信/邮箱验证码实现
 *
 * <p>存储结构：{@code scene + ":" + target} → 验证码、过期时间、重发时间、已尝试次数。 验证码一次性使用；同一目标同一场景 60 秒内不能重发；连续校验失败 5
 * 次即作废。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthProperties authProperties;

    private final SmsSender smsSender;

    private final EmailCodeSender emailCodeSender;

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public String send(CodeScene scene, CodeChannel channel, String target) {
        String normalized = normalizeTarget(target);
        validateTarget(channel, normalized);

        AuthProperties.Code config = authProperties.getCode();
        String key = key(scene, normalized);
        String code = generateCode();
        Entry newEntry;

        synchronized (this) {
            cleanupExpired();
            Entry existing = store.get(key);
            long now = System.currentTimeMillis();
            if (existing != null && now - existing.sentAt < config.getResendSeconds() * 1000L) {
                long waitSeconds =
                        Math.max(
                                1,
                                (config.getResendSeconds() * 1000L - (now - existing.sentAt) + 999)
                                        / 1000);
                throw new BizException(ResultCode.BAD_REQUEST, "发送过于频繁，请 " + waitSeconds + " 秒后再试");
            }
            newEntry =
                    new Entry(
                            code,
                            now + config.getExpireMinutes() * 60_000L,
                            now,
                            0,
                            config.getMaxVerifyAttempts());
            store.put(key, newEntry);
        }

        try {
            if (channel == CodeChannel.SMS) {
                smsSender.send(normalized, code, scene, config.getExpireMinutes());
            } else {
                emailCodeSender.send(normalized, code, scene, config.getExpireMinutes());
            }
        } catch (RuntimeException e) {
            // 发送失败时释放冷却，允许用户立即重试；只删除本次写入的条目，避免误删并发重发的新验证码
            store.remove(key, newEntry);
            throw e;
        }
        log.info(
                "验证码已发送: scene={}, channel={}, provider={}",
                scene,
                channel,
                channel == CodeChannel.SMS
                        ? smsSender.providerName()
                        : emailCodeSender.providerName());
        return code;
    }

    @Override
    public boolean verify(CodeScene scene, String target, String code) {
        String normalized = normalizeTarget(target);
        String key = key(scene, normalized);
        synchronized (this) {
            Entry entry = store.get(key);
            if (entry == null) {
                return false;
            }
            if (entry.expireAt < System.currentTimeMillis()) {
                store.remove(key);
                return false;
            }
            byte[] expected = entry.code.getBytes(StandardCharsets.UTF_8);
            byte[] actual =
                    code == null ? new byte[0] : code.trim().getBytes(StandardCharsets.UTF_8);
            if (MessageDigest.isEqual(expected, actual)) {
                store.remove(key);
                return true;
            }
            int attempts = entry.attempts + 1;
            if (attempts >= entry.maxAttempts) {
                store.remove(key);
                log.warn("验证码校验超过最大次数，已作废: scene={}", scene);
            } else {
                store.put(
                        key,
                        new Entry(
                                entry.code,
                                entry.expireAt,
                                entry.sentAt,
                                attempts,
                                entry.maxAttempts));
            }
            return false;
        }
    }

    @Override
    public CodeChannel detectChannel(String target) {
        String normalized = normalizeTarget(target);
        if (EMAIL_PATTERN.matcher(normalized).matches()) {
            return CodeChannel.EMAIL;
        }
        if (PHONE_PATTERN.matcher(normalized).matches()) {
            return CodeChannel.SMS;
        }
        throw new BizException(ResultCode.BAD_REQUEST, "请输入正确的手机号或邮箱");
    }

    @Override
    public String normalizeTarget(String target) {
        if (target == null) {
            return "";
        }
        String normalized = target.trim();
        return normalized.contains("@") ? normalized.toLowerCase(Locale.ROOT) : normalized;
    }

    @Override
    public boolean isContact(String target) {
        String normalized = normalizeTarget(target);
        return PHONE_PATTERN.matcher(normalized).matches()
                || EMAIL_PATTERN.matcher(normalized).matches();
    }

    private void validateTarget(CodeChannel channel, String target) {
        boolean valid =
                switch (channel) {
                    case SMS -> PHONE_PATTERN.matcher(target).matches();
                    case EMAIL -> EMAIL_PATTERN.matcher(target).matches();
                };
        if (!valid) {
            throw new BizException(ResultCode.BAD_REQUEST, "请输入正确的手机号或邮箱");
        }
    }

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String key(CodeScene scene, String target) {
        return scene.name() + ":" + target;
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> entry.getValue().expireAt < now);
    }

    private record Entry(String code, long expireAt, long sentAt, int attempts, int maxAttempts) {}
}
