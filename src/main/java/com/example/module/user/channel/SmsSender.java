package com.example.module.user.channel;

import com.example.module.user.enums.CodeScene;

/**
 * 短信发送通道（可插拔）
 *
 * <p>由 {@code auth.code.sms.provider} 选择实现：mock（默认，打印日志）或 http（自建短信网关）。
 */
public interface SmsSender {

    /**
     * 发送验证码短信
     *
     * @param phone 手机号（明文，实现内部负责脱敏日志）
     * @param code 验证码
     * @param scene 使用场景
     * @param expireMinutes 有效期（分钟）
     */
    void send(String phone, String code, CodeScene scene, int expireMinutes);

    /** 通道名称（日志/健康检查用） */
    String providerName();
}
