package com.example.module.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.common.BizException;
import com.example.config.AuthProperties;
import com.example.module.user.channel.EmailCodeSender;
import com.example.module.user.channel.SmsSender;
import com.example.module.user.enums.CodeChannel;
import com.example.module.user.enums.CodeScene;
import com.example.module.user.service.impl.VerificationCodeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 短信/邮箱验证码服务单元测试（一次性、限频、错误次数、通道识别） */
class VerificationCodeServiceImplTest {

    private AuthProperties properties;

    private RecordingSmsSender smsSender;

    private RecordingEmailSender emailSender;

    private VerificationCodeServiceImpl codeService;

    @BeforeEach
    void setUp() {
        properties = new AuthProperties();
        properties.getCode().setResendSeconds(60);
        properties.getCode().setExpireMinutes(5);
        properties.getCode().setMaxVerifyAttempts(5);
        smsSender = new RecordingSmsSender();
        emailSender = new RecordingEmailSender();
        codeService = new VerificationCodeServiceImpl(properties, smsSender, emailSender);
    }

    @Test
    @DisplayName("短信验证码：发送后校验一次通过，再次校验失败")
    void sendAndVerifyOnce() {
        String code = codeService.send(CodeScene.LOGIN, CodeChannel.SMS, "13800000000");

        assertTrue(code.matches("\\d{6}"));
        assertEquals(code, smsSender.lastCode);
        assertEquals("13800000000", smsSender.lastTarget);
        assertTrue(codeService.verify(CodeScene.LOGIN, "13800000000", code));
        assertFalse(codeService.verify(CodeScene.LOGIN, "13800000000", code));
    }

    @Test
    @DisplayName("同目标同场景 60 秒内不能重复发送")
    void resendCooldown() {
        codeService.send(CodeScene.LOGIN, CodeChannel.SMS, "13800000000");

        BizException e =
                assertThrows(
                        BizException.class,
                        () -> codeService.send(CodeScene.LOGIN, CodeChannel.SMS, "13800000000"));
        assertTrue(e.getMessage().contains("发送过于频繁"));
        // 不同场景不受影响
        codeService.send(CodeScene.RESET_PASSWORD, CodeChannel.SMS, "13800000000");
    }

    @Test
    @DisplayName("连续输错达到上限后验证码作废，正确验证码也不能再用")
    void wrongAttemptsInvalidateCode() {
        String code = codeService.send(CodeScene.LOGIN, CodeChannel.SMS, "13800000000");
        String wrong = (code.charAt(0) == '0' ? "1" : "0") + code.substring(1);

        for (int i = 0; i < 5; i++) {
            assertFalse(codeService.verify(CodeScene.LOGIN, "13800000000", wrong));
        }
        assertFalse(codeService.verify(CodeScene.LOGIN, "13800000000", code));
    }

    @Test
    @DisplayName("邮箱验证码走邮件通道，邮箱归一化为小写")
    void emailChannel() {
        String code =
                codeService.send(CodeScene.REGISTER, CodeChannel.EMAIL, "Student@Example.COM");

        assertEquals("student@example.com", emailSender.lastTarget);
        assertEquals(code, emailSender.lastCode);
        assertEquals(CodeChannel.EMAIL, codeService.detectChannel("Student@Example.COM"));
        assertTrue(codeService.verify(CodeScene.REGISTER, "student@example.com", code));
    }

    @Test
    @DisplayName("通道识别与非法联系方式校验")
    void detectChannelAndInvalidTarget() {
        assertEquals(CodeChannel.SMS, codeService.detectChannel("13800000000"));
        assertEquals(CodeChannel.EMAIL, codeService.detectChannel("a@b.com"));
        assertEquals("a@b.com", codeService.normalizeTarget(" A@B.COM "));
        assertThrows(BizException.class, () -> codeService.detectChannel("not-a-contact"));
    }

    private static class RecordingSmsSender implements SmsSender {
        private String lastTarget;
        private String lastCode;

        @Override
        public void send(String phone, String code, CodeScene scene, int expireMinutes) {
            this.lastTarget = phone;
            this.lastCode = code;
        }

        @Override
        public String providerName() {
            return "test";
        }
    }

    private static class RecordingEmailSender implements EmailCodeSender {
        private String lastTarget;
        private String lastCode;

        @Override
        public void send(String email, String code, CodeScene scene, int expireMinutes) {
            this.lastTarget = email;
            this.lastCode = code;
        }

        @Override
        public String providerName() {
            return "test";
        }
    }
}
