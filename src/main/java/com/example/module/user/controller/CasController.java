package com.example.module.user.controller;

import com.example.common.BizException;
import com.example.common.Result;
import com.example.config.AuthProperties;
import com.example.module.user.dto.CasExchangeRequest;
import com.example.module.user.dto.LoginResponse;
import com.example.module.user.service.AuthService;
import com.example.module.user.service.CasService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 统一身份认证（CAS）接口
 *
 * <ul>
 *   <li>GET /api/v1/auth/cas/login —— 跳转统一身份认证
 *   <li>GET /api/v1/auth/cas/mock-login —— 模拟 CAS 登录页（仅 mock 模式）
 *   <li>POST /api/v1/auth/cas/mock-login —— 模拟 CAS 登录提交（仅 mock 模式）
 *   <li>GET /api/v1/auth/cas/callback —— CAS 回调，换发一次性本地票据
 *   <li>POST /api/v1/auth/cas/exchange —— 用一次性票据换取 JWT
 * </ul>
 */
@Controller
@RequestMapping("/api/v1/auth/cas")
@RequiredArgsConstructor
public class CasController {

    private final CasService casService;

    private final AuthService authService;

    private final AuthProperties authProperties;

    /** 跳转统一身份认证（mock 模式跳内置登录页，real 模式跳学校 CAS） */
    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        return redirect(casService.startLogin());
    }

    /** 模拟 CAS 登录页 */
    @GetMapping("/mock-login")
    public ResponseEntity<String> mockLoginPage(
            @RequestParam String service, @RequestParam String state) {
        requireMockMode();
        return html(renderMockLoginPage(service, state, null));
    }

    /** 模拟 CAS 登录提交：验证本地学号/工号密码，签发 CAS ticket 并回跳 */
    @PostMapping("/mock-login")
    public ResponseEntity<String> mockLoginSubmit(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String service,
            @RequestParam String state) {
        requireMockMode();
        try {
            String location = casService.mockLogin(username, password, service);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(location))
                    .<String>build();
        } catch (BizException e) {
            // 浏览器表单提交，直接回显错误页；仍返回 200，避免浏览器显示默认错误页
            return html(renderMockLoginPage(service, state, e.getMessage()));
        }
    }

    /** CAS 回调：校验 state + ticket，成功后向前端下发一次性本地票据 */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String ticket,
            @RequestParam(required = false) String state) {
        try {
            String username = casService.completeCallback(ticket, state);
            String exchangeTicket = casService.issueExchangeTicket(username);
            return redirect(
                    UriComponentsBuilder.fromUriString(casService.getFrontendCallbackUrl())
                            .queryParam("ticket", exchangeTicket)
                            .build()
                            .encode()
                            .toUriString());
        } catch (BizException e) {
            return redirect(
                    UriComponentsBuilder.fromUriString(casService.getFrontendCallbackUrl())
                            .queryParam("error", e.getMessage())
                            .build()
                            .encode()
                            .toUriString());
        }
    }

    /** 前端用一次性票据换取 JWT（票据 2 分钟有效、仅可使用一次） */
    @PostMapping("/exchange")
    @ResponseBody
    public Result<LoginResponse> exchange(@Valid @RequestBody CasExchangeRequest request) {
        String username = casService.consumeExchangeTicket(request.getTicket());
        return Result.ok(authService.loginByCas(username));
    }

    private void requireMockMode() {
        if (!"mock".equalsIgnoreCase(authProperties.getCas().getMode())) {
            throw new BizException(com.example.common.ResultCode.NOT_FOUND, "模拟统一身份认证已关闭");
        }
    }

    private ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build();
    }

    private ResponseEntity<String> html(String body) {
        return ResponseEntity.ok()
                .contentType(
                        new MediaType(MediaType.TEXT_HTML, java.nio.charset.StandardCharsets.UTF_8))
                .body(body);
    }

    /** 内置模拟 CAS 登录页（样式保持简洁，明确标注「模拟」避免误用） */
    private String renderMockLoginPage(String service, String state, String error) {
        String errorHtml =
                StringUtils.hasText(error)
                        ? "<div class=\"error\">" + HtmlUtils.htmlEscape(error) + "</div>"
                        : "";
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>统一身份认证（模拟）</title>
                  <style>
                    * { box-sizing: border-box; }
                    body { margin:0; min-height:100vh; display:flex; align-items:center; justify-content:center;
                           background:linear-gradient(135deg,#0f2e4d,#1b6b93 55%,#2fa5a5); font-family:"Microsoft YaHei",system-ui,sans-serif; }
                    .card { width:400px; background:#fff; border-radius:14px; padding:34px 36px 30px;
                            box-shadow:0 18px 50px rgba(0,0,0,.28); }
                    h1 { margin:0 0 6px; font-size:21px; color:#17324d; }
                    .sub { color:#7b8a9a; font-size:13px; margin-bottom:22px; }
                    .tag { display:inline-block; background:#e8f4ff; color:#1b6b93; border-radius:20px;
                           padding:2px 10px; font-size:12px; margin-left:6px; vertical-align:2px; }
                    label { display:block; font-size:13px; color:#33475b; margin:14px 0 6px; }
                    input { width:100%; height:42px; border:1px solid #d5dee7; border-radius:8px; padding:0 12px;
                            font-size:14px; outline:none; }
                    input:focus { border-color:#1b6b93; box-shadow:0 0 0 3px rgba(27,107,147,.12); }
                    button { width:100%; height:44px; margin-top:22px; border:0; border-radius:8px; cursor:pointer;
                             background:linear-gradient(135deg,#1b6b93,#2fa5a5); color:#fff; font-size:15px; }
                    button:hover { filter:brightness(1.05); }
                    .error { background:#fff1f0; border:1px solid #ffccc7; color:#cf1322; border-radius:8px;
                             padding:9px 12px; font-size:13px; margin-bottom:10px; }
                    .hint { margin-top:18px; font-size:12px; color:#9aa7b4; line-height:1.7; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h1>统一身份认证<span class="tag">模拟</span></h1>
                    <div class="sub">师范专业 AI 试讲台 · 单点登录演示</div>
                    __ERROR__
                    <form method="post" action="/api/v1/auth/cas/mock-login">
                      <input type="hidden" name="service" value="__SERVICE__">
                      <input type="hidden" name="state" value="__STATE__">
                      <label>学号 / 工号</label>
                      <input name="username" autocomplete="username" autofocus required>
                      <label>密码</label>
                      <input name="password" type="password" autocomplete="current-password" required>
                      <button type="submit">登录</button>
                    </form>
                    <div class="hint">开发环境演示账号：admin / admin123<br>本页面用于模拟学校 CAS，生产环境请配置 auth.cas.mode=real。</div>
                  </div>
                </body>
                </html>
                """
                .replace("__ERROR__", errorHtml)
                .replace("__SERVICE__", HtmlUtils.htmlEscape(service))
                .replace("__STATE__", HtmlUtils.htmlEscape(state));
    }
}
