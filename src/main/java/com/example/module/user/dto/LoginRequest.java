package com.example.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 登录请求（POST /api/v1/auth/login） */
@Data
public class LoginRequest {

    /** 登录名（学号/工号） */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 明文密码（服务端与 BCrypt 哈希比对，不落库） */
    @NotBlank(message = "密码不能为空")
    private String password;
}
