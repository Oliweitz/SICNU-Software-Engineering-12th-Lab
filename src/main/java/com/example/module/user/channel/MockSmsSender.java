package com.example.module.user.channel;

import com.example.common.util.MaskUtil;
import com.example.module.user.enums.CodeScene;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 模拟短信通道（默认）
 *
 * <p>不产生真实短信费用，验证码打印到后端日志，便于开发联调与课堂演示。 配置 {@code auth.code.sms.provider=http} 且提供网关地址后切换真实通道。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "auth.code.sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsSender implements SmsSender {

    @Override
    public void send(String phone, String code, CodeScene scene, int expireMinutes) {
        log.info(
                "[MOCK-SMS] 手机号={} 场景={} 验证码={}（{} 分钟内有效）",
                MaskUtil.maskPhone(phone),
                scene.getLabel(),
                code,
                expireMinutes);
    }

    @Override
    public String providerName() {
        return "mock";
    }
}
