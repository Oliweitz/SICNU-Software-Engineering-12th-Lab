package com.example.module.user.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 登录响应：令牌 + 身份 + 授权（前端登录后即可渲染菜单，无需再调 /rbac/me） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** JWT 令牌（请求头 Authorization: Bearer <token>） */
    private String token;

    /** 用户 id */
    private Long userId;

    /** 登录名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 角色码列表 */
    private List<String> roles;

    /** 权限码列表 */
    private List<String> permissions;
}
