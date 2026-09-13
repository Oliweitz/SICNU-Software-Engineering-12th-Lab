package com.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Web MVC 配置 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 开发期 CORS 放行：前端 dev 服务器（Vite 默认 5173 端口）跨域访问后端
     *
     * <p>生产/演示部署时前端静态资源与后端同域或经反向代理，应关闭此项 （见 application-prod.yml 中可覆盖配置）。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
