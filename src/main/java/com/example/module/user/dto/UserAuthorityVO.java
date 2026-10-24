package com.example.module.user.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前用户授权视图（GET /api/v1/rbac/me）
 *
 * <p>前端据此控制菜单与按钮显隐；权限码为准，角色码仅用于界面上的身份标识。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthorityVO {

    /** 用户 id */
    private Long userId;

    /** 登录名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 角色码列表（STUDENT / TEACHER / ADMIN） */
    private List<String> roles;

    /** 权限码列表 */
    private List<String> permissions;
}
