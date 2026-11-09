package com.example.module.user.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.example.common.BizException;
import com.example.common.ResultCode;
import com.example.config.AuthProperties;
import com.example.module.user.dto.CaptchaVO;
import com.example.module.user.service.CaptchaService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 图形验证码实现（Hutool 线段干扰验证码）
 *
 * <p>验证码存内存，一次性使用、过期自动清理；不依赖 Redis，符合课程项目「可选缓存」的约束。 分布式部署时把 {@code store} 换成 Redis 即可。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private final AuthProperties authProperties;

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public CaptchaVO generate() {
        cleanupExpired();
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(130, 44, 4, 20);
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        int expireMinutes = authProperties.getCaptcha().getExpireMinutes();
        String code = captcha.getCode().toUpperCase(Locale.ROOT);
        store.put(captchaId, new Entry(code, System.currentTimeMillis() + expireMinutes * 60_000L));
        String debugCode = authProperties.getCaptcha().isExposeInResponse() ? code : null;
        return new CaptchaVO(
                captchaId, captcha.getImageBase64Data(), expireMinutes * 60, debugCode);
    }

    @Override
    public void verify(String captchaId, String captchaCode) {
        Entry entry = captchaId == null ? null : store.remove(captchaId);
        boolean valid =
                entry != null
                        && entry.expireAt >= System.currentTimeMillis()
                        && captchaCode != null
                        && MessageDigest.isEqual(
                                entry.code.getBytes(StandardCharsets.UTF_8),
                                captchaCode
                                        .trim()
                                        .toUpperCase(Locale.ROOT)
                                        .getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            log.warn("图形验证码校验失败: captchaId={}", captchaId);
            throw new BizException(ResultCode.BAD_REQUEST, "图形验证码错误或已过期");
        }
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> entry.getValue().expireAt < now);
    }

    private record Entry(String code, long expireAt) {}
}
