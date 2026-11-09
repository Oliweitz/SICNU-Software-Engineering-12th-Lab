package com.example.module.user.channel;

import com.example.module.user.enums.CodeScene;

/**
 * 邮箱验证码发送通道（可插拔）
 *
 * <p>由 {@code auth.code.mail.provider} 选择实现：mock（默认，打印日志）或 smtp（spring.mail.*）。
 */
public interface EmailCodeSender {

    /**
     * 发送验证码邮件
     *
     * @param email 收件邮箱（明文，实现内部负责脱敏日志）
     * @param code 验证码
     * @param scene 使用场景
     * @param expireMinutes 有效期（分钟）
     */
    void send(String email, String code, CodeScene scene, int expireMinutes);

    /** 通道名称 */
    String providerName();
}
