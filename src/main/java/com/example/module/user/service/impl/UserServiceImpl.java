package com.example.module.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.common.BizException;
import com.example.common.ResultCode;
import com.example.module.user.dto.LoginRequest;
import com.example.module.user.dto.LoginResponse;
import com.example.module.user.dto.UserAuthorityVO;
import com.example.module.user.entity.SysUser;
import com.example.module.user.mapper.UserMapper;
import com.example.module.user.service.RbacService;
import com.example.module.user.service.UserService;
import com.example.security.JwtUtil;
import com.example.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户业务实现（登录）
 *
 * <p>分层约束见 docs/代码规范.md 第 2 节：业务逻辑在 Service，Controller 只做参数校验与返回 Result。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final RbacService rbacService;

    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user =
                userMapper.selectOne(
                        Wrappers.<SysUser>lambdaQuery()
                                .eq(SysUser::getUsername, request.getUsername()));
        // 用户不存在与密码错误返回同一提示，避免账号枚举
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("登录失败: username={}", request.getUsername());
            throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        UserAuthorityVO authority = rbacService.loadAuthority(user);
        LoginUser loginUser =
                LoginUser.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .realName(user.getRealName())
                        .roles(authority.getRoles())
                        .permissions(authority.getPermissions())
                        .build();
        String token = jwtUtil.generateToken(loginUser);

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .roles(authority.getRoles())
                .permissions(authority.getPermissions())
                .build();
    }
}
