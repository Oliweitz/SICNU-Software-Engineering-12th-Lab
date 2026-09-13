package com.example.security;

import com.example.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录用户上下文（JWT 载荷与线程上下文中传递）
 *
 * <p>骨架阶段先落地结构；迭代 1 由 {@code JwtUtil} 签发/解析时填充。
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

    /** 角色 */
    private Role role;

    /** 班级 id（师范生角色才有） */
    private Long clazzId;
}
