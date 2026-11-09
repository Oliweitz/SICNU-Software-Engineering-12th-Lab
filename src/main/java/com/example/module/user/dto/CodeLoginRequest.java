package com.example.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 验证码登录请求（POST /api/v1/auth/login/code）：手机号或邮箱 + 验证码 */
@Data
public class CodeLoginRequest {

    /** 手机号或邮箱（含 @ 视为邮箱） */
    @NotBlank(message = "手机号或邮箱不能为空")
    private String target;

    /** 验证码 */
    @NotBlank(message = "验证码不能为空")
    private String code;
}
