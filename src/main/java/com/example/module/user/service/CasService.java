package com.example.module.user.service;

/**
 * 统一身份认证（CAS）服务
 *
 * <p>支持两种模式：
 *
 * <ul>
 *   <li>{@code mock}：内置模拟 CAS 登录页，用于本地开发与课堂演示
 *   <li>{@code real}：对接学校 CAS Server，回调后调用 serviceValidate 校验 ticket
 * </ul>
 *
 * <p>无论哪种模式，回调成功后只向前端下发一次性本地票据，前端再用它换取 JWT， 避免把长期令牌暴露在浏览器地址栏与访问日志中。
 */
public interface CasService {

    /**
     * 开始统一身份认证
     *
     * @return 浏览器应跳转的地址（模拟登录页或真实 CAS 登录地址）
     */
    String startLogin();

    /**
     * 模拟 CAS 登录提交（仅 mock 模式）
     *
     * @return 携带 CAS ticket 的回调地址
     */
    String mockLogin(String username, String password, String service);

    /**
     * 处理 CAS 回调：校验 state 与 ticket
     *
     * @return 本地学号/工号
     */
    String completeCallback(String ticket, String state);

    /** 回调成功后签发一次性本地票据（有效期 2 分钟） */
    String issueExchangeTicket(String username);

    /** 前端用本地票据换取 JWT 前，消费票据并返回学号/工号 */
    String consumeExchangeTicket(String ticket);

    /** 前端统一认证回调路由 */
    String getFrontendCallbackUrl();
}
