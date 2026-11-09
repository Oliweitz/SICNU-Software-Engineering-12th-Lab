package com.example.module.user.channel;

import com.example.common.BizException;
import com.example.common.ResultCode;
import com.example.common.util.MaskUtil;
import com.example.config.AuthProperties;
import com.example.module.user.enums.CodeScene;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP 邮件通道
 *
 * <p>依赖 Spring Boot 的 {@code spring.mail.*} 配置（host/port/username/password）。 未配置时应用仍可启动（默认走 mock
 * 通道），仅在真实发送时报错。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "auth.code.mail.provider", havingValue = "smtp")
public class SmtpEmailCodeSender implements EmailCodeSender {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    private final AuthProperties authProperties;

    @Override
    public void send(String email, String code, CodeScene scene, int expireMinutes) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "邮件通道未配置（spring.mail.host），请联系管理员");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(authProperties.getCode().getMail().getFrom());
        message.setTo(email);
        message.setSubject("【AI 试讲台】" + scene.getLabel() + "验证码");
        message.setText(
                "您的"
                        + scene.getLabel()
                        + "验证码是："
                        + code
                        + "\n"
                        + "有效期 "
                        + expireMinutes
                        + " 分钟，请勿泄露给他人。\n"
                        + "如非本人操作，请忽略本邮件。");
        try {
            mailSender.send(message);
            log.info(
                    "邮件已发送: email={}, scene={}, provider=smtp",
                    MaskUtil.maskEmail(email),
                    scene.getLabel());
        } catch (MailException e) {
            log.error("邮件发送失败: email={}, scene={}", MaskUtil.maskEmail(email), scene.name(), e);
            throw new BizException(ResultCode.INTERNAL_ERROR, "验证码发送失败，请稍后重试");
        }
    }

    @Override
    public String providerName() {
        return "smtp";
    }
}
