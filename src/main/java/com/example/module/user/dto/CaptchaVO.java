package com.example.module.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 图形验证码响应（GET /api/v1/auth/captcha） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVO {

    /** 验证码标识，提交表单时原样带回 */
    private String captchaId;

    /** data:image/png;base64,... 形式，可直接作为 img src */
    private String image;

    /** 有效期（秒） */
    private int expireSeconds;

    /** 开发环境回显的验证码明文（生产恒为 null，由 auth.captcha.expose-in-response 控制） */
    private String debugCode;
}
