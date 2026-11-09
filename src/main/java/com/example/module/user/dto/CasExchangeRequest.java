package com.example.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 统一身份认证票据换取 JWT 请求（POST /api/v1/auth/cas/exchange） */
@Data
public class CasExchangeRequest {

    /** 回调时签发的本地一次性票据（不在 URL 中直接暴露 JWT） */
    @NotBlank(message = "票据不能为空")
    private String ticket;
}
