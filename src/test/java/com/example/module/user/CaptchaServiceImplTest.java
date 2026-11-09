package com.example.module.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.common.BizException;
import com.example.config.AuthProperties;
import com.example.module.user.dto.CaptchaVO;
import com.example.module.user.service.impl.CaptchaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 图形验证码服务单元测试（一次性、过期、错误码） */
class CaptchaServiceImplTest {

    private AuthProperties properties;

    private CaptchaServiceImpl captchaService;

    @BeforeEach
    void setUp() {
        properties = new AuthProperties();
        properties.getCaptcha().setExposeInResponse(true);
        captchaService = new CaptchaServiceImpl(properties);
    }

    @Test
    @DisplayName("生成的验证码带唯一标识与 Base64 图片，正确输入可通过")
    void generateAndVerify() {
        CaptchaVO captcha = captchaService.generate();

        assertNotNull(captcha.getCaptchaId());
        assertNotNull(captcha.getImage());
        assertTrue(captcha.getImage().startsWith("data:image/png;base64,"));
        assertTrue(captcha.getExpireSeconds() > 0);
        assertNotNull(captcha.getDebugCode());

        captchaService.verify(captcha.getCaptchaId(), captcha.getDebugCode().toLowerCase());
    }

    @Test
    @DisplayName("图形验证码一次性使用：通过后立即作废")
    void captchaIsSingleUse() {
        CaptchaVO captcha = captchaService.generate();
        captchaService.verify(captcha.getCaptchaId(), captcha.getDebugCode());

        assertThrows(
                BizException.class,
                () -> captchaService.verify(captcha.getCaptchaId(), captcha.getDebugCode()));
    }

    @Test
    @DisplayName("错误图形验证码被拒绝")
    void wrongCodeRejected() {
        CaptchaVO captcha = captchaService.generate();

        BizException e =
                assertThrows(
                        BizException.class,
                        () -> captchaService.verify(captcha.getCaptchaId(), "wrong"));
        assertEquals("图形验证码错误或已过期", e.getMessage());
    }

    @Test
    @DisplayName("过期图形验证码被拒绝")
    void expiredCodeRejected() {
        properties.getCaptcha().setExpireMinutes(-1);
        CaptchaVO captcha = captchaService.generate();

        assertThrows(
                BizException.class,
                () -> captchaService.verify(captcha.getCaptchaId(), captcha.getDebugCode()));
    }

    @Test
    @DisplayName("生产配置不回显验证码明文")
    void debugCodeHiddenWhenNotExposed() {
        properties.getCaptcha().setExposeInResponse(false);
        CaptchaVO captcha = captchaService.generate();

        assertNull(captcha.getDebugCode());
    }
}
