package com.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * 骨架冒烟测试：验证 Spring 上下文可正常加载（见 docs/代码规范.md 第 8 节）
 *
 * <p>实现说明：刻意不使用 {@code @SpringBootTest}（SpringExtension 会初始化 Mockito， 其内嵌 inline mock maker 在 JDK
 * 23+ 上依赖 agent 动态 attach，本机 JDK 24 环境下 初始化必然失败）。需要 Mock 功能的测试留待迭代 2 起编写，届时需先解决该环境问题 （升级 Byte Buddy
 * 或改用兼容 JDK）。
 *
 * <p>无需 MySQL：application-dev.yml 中 hikari initialization-fail-timeout=-1， 连接池懒初始化，上下文加载不校验数据库连通。
 */
class ApplicationSmokeTest {

    @Test
    @DisplayName("骨架可启动：Spring 上下文加载成功")
    void contextLoads() {
        // Web 类型置 NONE：只验证 IoC 容器与各配置类装配，不绑定 8080 端口
        try (var context =
                new SpringApplicationBuilder(Application.class)
                        .web(WebApplicationType.NONE)
                        .run()) {
            // 空实现：上下文加载成功即通过
        }
    }
}
