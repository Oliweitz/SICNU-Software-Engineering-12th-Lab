package com.example.module.user.channel;

import com.example.common.util.MaskUtil;
import com.example.module.user.enums.CodeScene;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 模拟邮件通道（默认）
 *
 * <p>验证码打印到后端日志；配置 {@code auth.code.mail.provider=smtp} 并填写 {@code spring.mail.*} 后切换真实发信。
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "auth.code.mail.provider",
        havingValue = "mock",
        matchIfMissing = true)
public class MockEmailCodeSender implements EmailCodeSender {

    @Override
    public void send(String email, String code, CodeScene scene, int expireMinutes) {
        log.info(
                "[MOCK-MAIL] 邮箱={} 场景={} 验证码={}（{} 分钟内有效）",
                MaskUtil.maskEmail(email),
                scene.getLabel(),
                code,
                expireMinutes);
    }

    @Override
    public String providerName() {
        return "mock";
    }
}
