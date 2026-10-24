package com.example.security;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录用户上下文（JWT 载荷与线程上下文中传递）
 *
 * <p>令牌只承载身份（id / username），角色与权限码在每次请求时由 {@code RbacService} 现查 ——授权变更即时生效，无需等旧令牌过期。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    /** 用户主键（sys_user.id） */
    private Long id;

    /** 登录名（学号/工号） */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 角色码列表（STUDENT / TEACHER / ADMIN，来自 sys_user_role） */
    private List<String> roles;

    /** 权限码列表（来自 sys_role_permission → sys_permission） */
    private List<String> permissions;
}
