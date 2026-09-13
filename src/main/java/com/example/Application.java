package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 师范专业 AI 试讲台系统 —— 启动类
 *
 * <p>包结构约定（详见 docs/技术方案.md 第 4 节）：
 *
 * <ul>
 *   <li>common/ 统一响应、异常、常量等横切组件
 *   <li>config/ Spring 配置类（Security、WebSocket、MyBatis-Plus、CORS）
 *   <li>security/ JWT 工具、认证过滤器、登录用户上下文
 *   <li>module/ 按业务域分包：user / clazz / task / trial / evaluate / report / review / resource / stats
 * </ul>
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
