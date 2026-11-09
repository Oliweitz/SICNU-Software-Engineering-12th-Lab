package com.example.module.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.common.BizException;
import com.example.common.ResultCode;
import com.example.common.enums.Role;
import com.example.common.util.MaskUtil;
import com.example.config.AuthProperties;
import com.example.module.user.dto.CodeLoginRequest;
import com.example.module.user.dto.LoginRequest;
import com.example.module.user.dto.LoginResponse;
import com.example.module.user.dto.RegisterRequest;
import com.example.module.user.dto.ResetPasswordRequest;
import com.example.module.user.dto.SendCodeRequest;
import com.example.module.user.dto.SendCodeResponse;
import com.example.module.user.dto.UserAuthorityVO;
import com.example.module.user.entity.SysUser;
import com.example.module.user.enums.CodeChannel;
import com.example.module.user.enums.CodeScene;
import com.example.module.user.mapper.UserMapper;
import com.example.module.user.service.AuthService;
import com.example.module.user.service.CaptchaService;
import com.example.module.user.service.LoginAttemptService;
import com.example.module.user.service.RbacService;
import com.example.module.user.service.VerificationCodeService;
import com.example.security.JwtUtil;
import com.example.security.LoginUser;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 认证业务实现
 *
 * <p>安全约定：
 *
 * <ul>
 *   <li>密码只存 BCrypt 哈希，校验时取盐后慢哈希重算比对；明文不落库、不打印日志
 *   <li>密码登录与验证码登录失败使用同一提示，避免账号枚举
 *   <li>找回密码发送验证码时不区分账号是否存在，统一返回「如果已绑定…」
 *   <li>手机号/邮箱在日志与响应中一律脱敏
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String PASSWORD_LOGIN_FAILED = "用户名或密码错误";

    private static final String CODE_LOGIN_FAILED = "手机号/邮箱或验证码错误";

    private static final String CODE_INVALID = "验证码错误或已过期";

    private static final String SEND_CODE_GENERIC = "如果该账号已绑定联系方式，验证码已发送，请注意查收";

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final RbacService rbacService;

    private final JwtUtil jwtUtil;

    private final CaptchaService captchaService;

    private final VerificationCodeService verificationCodeService;

    private final LoginAttemptService loginAttemptService;

    private final AuthProperties authProperties;

    @Override
    public LoginResponse login(LoginRequest request) {
        captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());

        String username = request.getUsername().trim();
        loginAttemptService.checkLocked(username);
        SysUser user = findByUsername(username);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.recordFailure(username);
            log.warn("密码登录失败: username={}", username);
            throw new BizException(ResultCode.UNAUTHORIZED, PASSWORD_LOGIN_FAILED);
        }
        loginAttemptService.reset(username);
        log.info("密码登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse loginByCode(CodeLoginRequest request) {
        CodeChannel channel = verificationCodeService.detectChannel(request.getTarget());
        String target = verificationCodeService.normalizeTarget(request.getTarget());
        SysUser user = findByContact(channel, target);
        if (user == null
                || !verificationCodeService.verify(CodeScene.LOGIN, target, request.getCode())) {
            log.warn("验证码登录失败: channel={}, target={}", channel, MaskUtil.maskContact(target));
            throw new BizException(ResultCode.UNAUTHORIZED, CODE_LOGIN_FAILED);
        }
        loginAttemptService.reset(user.getUsername());
        log.info(
                "验证码登录成功: userId={}, channel={}, target={}",
                user.getId(),
                channel,
                MaskUtil.maskContact(target));
        return buildLoginResponse(user);
    }

    @Override
    public SendCodeResponse sendCode(SendCodeRequest request) {
        captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());

        CodeChannel channel = verificationCodeService.detectChannel(request.getTarget());
        String target = verificationCodeService.normalizeTarget(request.getTarget());
        SysUser user = findByContact(channel, target);
        CodeScene scene = request.getScene();
        int resendSeconds = authProperties.getCode().getResendSeconds();

        if (scene == CodeScene.REGISTER) {
            if (user != null) {
                throw new BizException(
                        ResultCode.CONFLICT, channel == CodeChannel.SMS ? "该手机号已被注册" : "该邮箱已被注册");
            }
        } else if (user == null) {
            // 防账号枚举：账号不存在时不实际发送，但仍返回统一提示
            log.info("验证码请求：目标未绑定账号（不发送）, scene={}, channel={}", scene, channel);
            return new SendCodeResponse(SEND_CODE_GENERIC, resendSeconds, null);
        }

        String code = verificationCodeService.send(scene, channel, target);
        String debugCode = authProperties.getCode().isExposeInResponse() ? code : null;
        // 注册场景不存在账号枚举问题，给出明确提示；登录/找回密码仍使用统一话术
        String message = scene == CodeScene.REGISTER ? "验证码已发送，请注意查收" : SEND_CODE_GENERIC;
        return new SendCodeResponse(message, resendSeconds, debugCode);
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        captchaService.verify(request.getCaptchaId(), request.getCaptchaCode());
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BizException(ResultCode.BAD_REQUEST, "两次输入的密码不一致");
        }

        String username = request.getUsername().trim();
        String phone = request.getPhone().trim();
        String email =
                StringUtils.hasText(request.getEmail())
                        ? verificationCodeService.normalizeTarget(request.getEmail())
                        : null;

        if (findByUsername(username) != null) {
            throw new BizException(ResultCode.CONFLICT, "登录名已被占用");
        }
        if (findByPhone(phone) != null) {
            throw new BizException(ResultCode.CONFLICT, "该手机号已被注册");
        }
        if (email != null && findByEmail(email) != null) {
            throw new BizException(ResultCode.CONFLICT, "该邮箱已被注册");
        }

        if (!verificationCodeService.verify(CodeScene.REGISTER, phone, request.getSmsCode())) {
            throw new BizException(ResultCode.BAD_REQUEST, "短信验证码错误或已过期");
        }
        if (email != null) {
            if (!StringUtils.hasText(request.getEmailCode())
                    || !verificationCodeService.verify(
                            CodeScene.REGISTER, email, request.getEmailCode())) {
                throw new BizException(ResultCode.BAD_REQUEST, "邮箱验证码错误或已过期");
            }
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName().trim());
        user.setMajor(request.getMajor());
        user.setGrade(request.getGrade());
        user.setPhone(phone);
        user.setEmail(email);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BizException(ResultCode.CONFLICT, "登录名、手机号或邮箱已被占用");
        }
        rbacService.assignRole(user.getId(), Role.STUDENT);
        log.info(
                "用户注册成功: userId={}, username={}, phone={}, email={}",
                user.getId(),
                user.getUsername(),
                MaskUtil.maskPhone(phone),
                MaskUtil.maskEmail(email));
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BizException(ResultCode.BAD_REQUEST, "两次输入的密码不一致");
        }
        CodeChannel channel = verificationCodeService.detectChannel(request.getTarget());
        String target = verificationCodeService.normalizeTarget(request.getTarget());
        SysUser user = findByContact(channel, target);

        // 账号不存在与验证码错误使用同一提示，避免账号枚举
        if (user == null
                || !verificationCodeService.verify(
                        CodeScene.RESET_PASSWORD, target, request.getCode())) {
            log.warn("找回密码失败: channel={}, target={}", channel, MaskUtil.maskContact(target));
            throw new BizException(ResultCode.BAD_REQUEST, CODE_INVALID);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        loginAttemptService.reset(user.getUsername());
        log.info("密码重置成功: userId={}, channel={}", user.getId(), channel);
    }

    @Override
    public LoginResponse loginByCas(String username) {
        String normalized = username == null ? "" : username.trim();
        SysUser user = findByUsername(normalized);
        if (user == null) {
            log.warn("统一身份认证通过但本地未绑定账号: username={}", normalized);
            throw new BizException(ResultCode.UNAUTHORIZED, "统一身份认证通过，但本地未绑定该学号/工号，请联系管理员");
        }
        loginAttemptService.reset(user.getUsername());
        log.info("统一身份认证登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return buildLoginResponse(user);
    }

    private LoginResponse buildLoginResponse(SysUser user) {
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
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpireSeconds())
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phoneMasked(
                        StringUtils.hasText(user.getPhone())
                                ? MaskUtil.maskPhone(user.getPhone())
                                : null)
                .emailMasked(
                        StringUtils.hasText(user.getEmail())
                                ? MaskUtil.maskEmail(user.getEmail())
                                : null)
                .roles(authority.getRoles())
                .permissions(authority.getPermissions())
                .build();
    }

    private SysUser findByUsername(String username) {
        return userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
    }

    private SysUser findByPhone(String phone) {
        return userMapper.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getPhone, phone));
    }

    private SysUser findByEmail(String email) {
        return userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getEmail, email.toLowerCase(Locale.ROOT)));
    }

    private SysUser findByContact(CodeChannel channel, String target) {
        return channel == CodeChannel.SMS ? findByPhone(target) : findByEmail(target);
    }
}
