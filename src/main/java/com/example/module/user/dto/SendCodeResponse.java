package com.example.module.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 发送验证码响应 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendCodeResponse {

    /** 统一提示。找回密码场景不区分账号是否存在，避免账号枚举。 */
    private String message;

    /** 重发冷却秒数（前端倒计时用） */
    private int resendSeconds;

    /**
     * 开发环境回显的验证码（生产环境恒为 null）。
     *
     * <p>由 auth.code.expose-in-response 控制，默认关闭；仅用于本地联调与课堂演示。
     */
    private String debugCode;
}
