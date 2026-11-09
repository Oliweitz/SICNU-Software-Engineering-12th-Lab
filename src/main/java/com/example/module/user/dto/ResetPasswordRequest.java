package com.example.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 重置密码请求（POST /api/v1/auth/password/reset） */
@Data
public class ResetPasswordRequest {

    /** 注册时绑定的手机号或邮箱 */
    @NotBlank(message = "手机号或邮箱不能为空")
    private String target;

    /** 验证码 */
    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度须为 6-32 位")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
