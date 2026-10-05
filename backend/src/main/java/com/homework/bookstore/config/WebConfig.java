package com.homework.bookstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <h2>CORS 跨域配置</h2>
 *
 * <p>浏览器同源策略：协议+域名+端口任一不同即跨域。
 * 前端 Vite 跑在 {@code 5173}，后端 Spring Boot 跑在 {@code 8080}，端口不同，浏览器会拦截响应。
 *
 * <h3>这里允许什么？</h3>
 * <ul>
 *   <li><b>来源</b>：{@code http://127.0.0.1:5173} 与 {@code http://localhost:5173}</li>
 *   <li><b>方法</b>：GET / POST / PUT / PATCH / DELETE / OPTIONS</li>
 *   <li><b>路径</b>：仅 {@code /api/**}，静态资源不开放</li>
 *   <li><b>请求头</b>：全部允许（实际仅用到 {@code Content-Type}）</li>
 * </ul>
 *
 * <h3>注意：开发模式下其实走的是 Vite 代理</h3>
 * Vite 的 {@code server.proxy} 把 {@code /api} 请求在 5173 内部转发到 8080，
 * 浏览器看起来是同源；代理仍可能保留 Origin，请求需通过后端来源与方法校验。
 * 这份配置真正生效的场景是：
 * <ul>
 *   <li>生产部署：前端打包成静态文件挂在 Nginx 上、直接调后端 8080</li>
 *   <li>开发时绕开代理（如用 Postman / curl 跨域调用）</li>
 * </ul>
 * 写在这里是为了双保险。
 */
@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOriginPatterns(
                                "http://127.0.0.1:*",
                                "http://localhost:*"
                        )
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
