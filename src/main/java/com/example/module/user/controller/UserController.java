package com.example.module.user.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口（统一前缀 /api/v1，见 docs/技术方案.md 第 7 节）
 *
 * <p>TODO 迭代 1 实现：
 *
 * <ul>
 *   <li>POST /api/v1/auth/login 登录返回 JWT（或独立 AuthController）
 *   <li>GET /api/v1/users/{id} 用户信息
 *   <li>PUT /api/v1/users/{id} 修改个人信息
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {}
