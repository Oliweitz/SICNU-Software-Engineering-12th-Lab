package com.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证相关配置（application.yml 的 auth.* 节）
 *
 * <p>包含图形验证码、短信/邮箱验证码、登录失败锁定、统一身份认证（CAS）与演示账号初始化。 所有阈值都做成配置项，便于答辩现场演示与生产环境按实际情况调整。
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /** 图形验证码 */
    private Captcha captcha = new Captcha();

    /** 短信/邮箱验证码 */
    private Code code = new Code();

    /** 登录失败锁定 */
    private Login login = new Login();

    /** 统一身份认证（CAS） */
    private Cas cas = new Cas();

    /** 开发环境演示账号初始化 */
    private Seed seed = new Seed();

    @Data
    public static class Captcha {
        /** 有效期（分钟） */
        private int expireMinutes = 5;

        /** 是否在响应中回传验证码明文（仅开发环境开启，便于联调） */
        private boolean exposeInResponse = false;
    }

    @Data
    public static class Code {
        /** 有效期（分钟） */
        private int expireMinutes = 5;

        /** 同一目标、同一场景的重发冷却（秒） */
        private int resendSeconds = 60;

        /** 单个验证码最大校验次数 */
        private int maxVerifyAttempts = 5;

        /** 是否在响应中回传验证码（仅开发环境开启） */
        private boolean exposeInResponse = false;

        /** 短信通道 */
        private Sms sms = new Sms();

        /** 邮件通道 */
        private Mail mail = new Mail();
    }

    @Data
    public static class Sms {
        /** mock | http */
        private String provider = "mock";

        private String httpUrl = "";
        private String httpToken = "";
    }

    @Data
    public static class Mail {
        /** mock | smtp */
        private String provider = "mock";

        private String from = "no-reply@ai-trial.local";
    }

    @Data
    public static class Login {
        /** 连续失败多少次后锁定 */
        private int maxFailures = 5;

        /** 锁定分钟数 */
        private int lockMinutes = 15;
    }

    @Data
    public static class Cas {
        /** mock = 内置模拟统一身份认证；real = 对接学校 CAS */
        private String mode = "mock";

        /** 真实 CAS Server 根地址 */
        private String serverUrl = "";

        /** 回调地址 */
        private String serviceUrl = "http://localhost:8080/api/v1/auth/cas/callback";

        /** 前端统一认证回调路由 */
        private String frontendCallbackUrl = "http://localhost:5173/sso-callback";

        /** CAS ticket / 本地换取票据有效期（分钟） */
        private int ticketExpireMinutes = 2;

        /** state 有效期（分钟） */
        private int stateExpireMinutes = 5;
    }

    @Data
    public static class Seed {
        /** 是否创建演示账号 */
        private boolean enabled = false;
    }
}
