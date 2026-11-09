package com.example.module.user.dto;

import com.example.module.user.enums.CodeScene;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 发送短信/邮箱验证码请求（POST /api/v1/auth/code/send） */
@Data
public class SendCodeRequest {

    /** 手机号或邮箱（含 @ 视为邮箱） */
    @NotBlank(message = "手机号或邮箱不能为空")
    private String target;

    /** 使用场景：LOGIN / REGISTER / RESET_PASSWORD */
    @NotNull(message = "验证码场景不能为空")
    private CodeScene scene;

    /** 图形验证码标识（防止短信轰炸） */
    @NotBlank(message = "图形验证码不能为空")
    private String captchaId;

    /** 图形验证码 */
    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;
}
