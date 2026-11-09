package com.example.module.user.service;

/**
 * 登录失败锁定服务
 *
 * <p>按登录名计数，连续失败达到阈值后锁定一段时间。计数在内存中，服务重启清零； 校内规模（10^3 用户）足够，分布式部署时需换 Redis 实现。
 */
public interface LoginAttemptService {

    /** 若账号处于锁定期则抛出业务异常 */
    void checkLocked(String username);

    /** 记录一次失败；达到阈值后开始锁定 */
    void recordFailure(String username);

    /** 登录成功后清除失败计数 */
    void reset(String username);
}
