package com.example.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MyBatis-Plus 配置 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 分页插件：开启后 Mapper 方法参数传入 {@code IPage} 即自动分页
     *
     * <p>用法：{@code Page<User> page = new Page<>(pageNum, pageSize);} 返回结果用 {@link
     * com.example.common.PageResult#of} 转换。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
