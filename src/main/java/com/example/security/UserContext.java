package com.example.security;

/**
 * 当前登录用户上下文（ThreadLocal 持有，请求结束必须清理）
 *
 * <p>骨架阶段先落地存取方法；迭代 1 由 {@code JwtAuthenticationFilter} 在解析令牌后 调用 {@link #set(LoginUser)}，并在
 * finally 中调用 {@link #clear()} 防止线程池复用串号。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {}

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    /** 当前登录用户；未登录时返回 null */
    public static LoginUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
