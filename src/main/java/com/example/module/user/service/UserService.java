package com.example.module.user.service;

/**
 * 用户业务（见 docs/技术方案.md 第 2.1 / 7 节）
 *
 * <p>TODO 迭代 1 实现：
 *
 * <ul>
 *   <li>登录：校验密码（BCrypt）→ 签发 JWT（POST /api/v1/auth/login）
 *   <li>注册/管理员建号：用户名唯一校验 + 密码加密
 *   <li>用户信息查询与修改（师范生绑定专业/班级/课程）
 * </ul>
 */
public interface UserService {}
