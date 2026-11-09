package com.example.module.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.common.BizException;
import com.example.common.ResultCode;
import com.example.config.AuthProperties;
import com.example.module.user.entity.SysUser;
import com.example.module.user.mapper.UserMapper;
import com.example.module.user.service.CasService;
import com.example.module.user.service.LoginAttemptService;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * 统一身份认证（CAS）实现
 *
 * <p>mock 模式用本地 sys_user 的学号/工号 + 密码模拟学校统一认证；real 模式走 CAS 3.0 serviceValidate。state 与 ticket
 * 均为内存一次性凭证，过期自动清理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CasServiceImpl implements CasService {

    private static final Pattern CAS_USER_PATTERN =
            Pattern.compile("(?is)<(?:cas:)?user>(.*?)</(?:cas:)?user>");

    private final AuthProperties authProperties;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final LoginAttemptService loginAttemptService;

    private final Map<String, Long> states = new ConcurrentHashMap<>();

    private final Map<String, Ticket> casTickets = new ConcurrentHashMap<>();

    private final Map<String, Ticket> exchangeTickets = new ConcurrentHashMap<>();

    @Override
    public String startLogin() {
        cleanupExpired();
        AuthProperties.Cas cas = authProperties.getCas();
        String state = UUID.randomUUID().toString().replace("-", "");
        states.put(state, System.currentTimeMillis() + cas.getStateExpireMinutes() * 60_000L);

        String serviceWithState =
                UriComponentsBuilder.fromUriString(cas.getServiceUrl())
                        .queryParam("state", state)
                        .build()
                        .toUriString();

        String encodedService = UriUtils.encodeQueryParam(serviceWithState, StandardCharsets.UTF_8);
        if (isMockMode()) {
            return "/api/v1/auth/cas/mock-login?service="
                    + encodedService
                    + "&state="
                    + UriUtils.encodeQueryParam(state, StandardCharsets.UTF_8);
        }

        if (!StringUtils.hasText(cas.getServerUrl())) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "CAS Server 未配置");
        }
        String base = cas.getServerUrl().replaceAll("/+$", "");
        return base + "/login?service=" + encodedService;
    }

    @Override
    public String mockLogin(String username, String password, String service) {
        if (!isMockMode()) {
            throw new BizException(ResultCode.NOT_FOUND, "模拟统一身份认证已关闭");
        }
        validateService(service);
        String normalized = username == null ? "" : username.trim();
        loginAttemptService.checkLocked(normalized);
        SysUser user =
                userMapper.selectOne(
                        Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, normalized));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            loginAttemptService.recordFailure(normalized);
            log.warn("模拟统一身份认证失败: username={}", normalized);
            throw new BizException(ResultCode.UNAUTHORIZED, "学号/工号或密码错误");
        }
        loginAttemptService.reset(normalized);

        String ticket = "ST-" + UUID.randomUUID().toString().replace("-", "");
        casTickets.put(
                ticket,
                new Ticket(
                        user.getUsername(),
                        System.currentTimeMillis()
                                + authProperties.getCas().getTicketExpireMinutes() * 60_000L));
        log.info("模拟统一身份认证通过: userId={}, username={}", user.getId(), user.getUsername());
        return UriComponentsBuilder.fromUriString(service)
                .queryParam("ticket", ticket)
                .build()
                .toUriString();
    }

    @Override
    public String completeCallback(String ticket, String state) {
        if (!StringUtils.hasText(ticket) || !StringUtils.hasText(state)) {
            throw new BizException(ResultCode.UNAUTHORIZED, "统一身份认证回调参数不完整");
        }
        Long expireAt = states.remove(state);
        if (expireAt == null || expireAt < System.currentTimeMillis()) {
            throw new BizException(ResultCode.UNAUTHORIZED, "登录状态已过期，请重新登录");
        }

        String username =
                isMockMode() ? consumeMockTicket(ticket) : validateRealTicket(ticket, state);
        if (!StringUtils.hasText(username)) {
            throw new BizException(ResultCode.UNAUTHORIZED, "统一身份认证失败，请重试");
        }
        return username;
    }

    @Override
    public String issueExchangeTicket(String username) {
        cleanupExpired();
        String ticket = UUID.randomUUID().toString().replace("-", "");
        exchangeTickets.put(
                ticket,
                new Ticket(
                        username,
                        System.currentTimeMillis()
                                + authProperties.getCas().getTicketExpireMinutes() * 60_000L));
        return ticket;
    }

    @Override
    public String consumeExchangeTicket(String ticket) {
        Ticket entry = ticket == null ? null : exchangeTickets.remove(ticket);
        if (entry == null || entry.expireAt < System.currentTimeMillis()) {
            throw new BizException(ResultCode.UNAUTHORIZED, "登录票据无效或已过期，请重新登录");
        }
        return entry.username;
    }

    @Override
    public String getFrontendCallbackUrl() {
        return authProperties.getCas().getFrontendCallbackUrl();
    }

    private String consumeMockTicket(String ticket) {
        Ticket entry = casTickets.remove(ticket);
        if (entry == null || entry.expireAt < System.currentTimeMillis()) {
            throw new BizException(ResultCode.UNAUTHORIZED, "CAS 票据无效或已过期，请重新登录");
        }
        return entry.username;
    }

    /** 真实 CAS：按 CAS 3.0 协议调用 serviceValidate，解析 cas:user */
    private String validateRealTicket(String ticket, String state) {
        AuthProperties.Cas cas = authProperties.getCas();
        if (!StringUtils.hasText(cas.getServerUrl())) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "CAS Server 未配置");
        }
        String serviceWithState =
                UriComponentsBuilder.fromUriString(cas.getServiceUrl())
                        .queryParam("state", state)
                        .build()
                        .toUriString();
        String base = cas.getServerUrl().replaceAll("/+$", "");
        String validateUrl =
                base
                        + "/serviceValidate?ticket="
                        + UriUtils.encodeQueryParam(ticket, StandardCharsets.UTF_8)
                        + "&service="
                        + UriUtils.encodeQueryParam(serviceWithState, StandardCharsets.UTF_8);
        try {
            String xml = RestClient.create().get().uri(validateUrl).retrieve().body(String.class);
            Matcher matcher = CAS_USER_PATTERN.matcher(xml == null ? "" : xml);
            return matcher.find() ? HtmlUtils.htmlUnescape(matcher.group(1).trim()) : null;
        } catch (RestClientException e) {
            log.error("CAS serviceValidate 调用失败: ticket={}", ticket, e);
            throw new BizException(ResultCode.UNAUTHORIZED, "统一身份认证校验失败，请重试");
        }
    }

    /** 防开放重定向：service 必须与配置的回调地址同源同路径 */
    private void validateService(String service) {
        try {
            URI configured = URI.create(authProperties.getCas().getServiceUrl());
            URI requested = URI.create(service);
            boolean sameOriginAndPath =
                    Objects.equals(configured.getScheme(), requested.getScheme())
                            && Objects.equals(configured.getHost(), requested.getHost())
                            && configured.getPort() == requested.getPort()
                            && Objects.equals(configured.getPath(), requested.getPath());
            if (!sameOriginAndPath) {
                throw new BizException(ResultCode.BAD_REQUEST, "非法的回调地址");
            }
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.BAD_REQUEST, "非法的回调地址");
        }
    }

    private boolean isMockMode() {
        return "mock".equalsIgnoreCase(authProperties.getCas().getMode());
    }

    /** 清理过期的一次性凭证，避免长期运行时 Map 无限增长 */
    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        states.entrySet().removeIf(entry -> entry.getValue() < now);
        casTickets.entrySet().removeIf(entry -> entry.getValue().expireAt < now);
        exchangeTickets.entrySet().removeIf(entry -> entry.getValue().expireAt < now);
    }

    private record Ticket(String username, long expireAt) {}
}
