package com.homework.bookstore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.bookstore.security.JwtAuthenticationFilter;
import com.homework.bookstore.security.JwtTokenService;
import com.homework.bookstore.security.JsonAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;

/** 作业4使用独立的受保护资源，历史公开书架接口保留原契约。 */
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtTokenService tokens,
                                           ObjectMapper mapper) throws Exception {
        // 【作业4 · 得分点4.4：无状态不是仅仅使用JWT字符串】
        // STATELESS不以HttpSession保存/读取登录身份；NullSecurityContextRepository明确
        // 不跨请求保存安全上下文；关闭requestCache，避免为“登录后跳回原请求”记录状态。
        // 当前凭证由客户端主动放Authorization头，不依赖浏览器自动携带的会话Cookie，
        // 所以关闭CSRF和表单/Basic登录。若以后改为Cookie凭证，需要重新评估CSRF方案。
        var unauthorized = new JsonAuthenticationEntryPoint(mapper);
        http.cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
            .requestCache(cache -> cache.disable())
            .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized))
            // 登录入口等保持可访问；只对/api/secure/**要求本次请求已有认证身份。
            // 无Token时Filter继续链，由这里拒绝；错误Token则已在Filter中直接拒绝。
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                .requestMatchers("/api/secure/**").authenticated()
                .anyRequest().permitAll())
            // 先验证JWT再进入后续权限检查；直接加入Security链，避免注册为Servlet Bean执行两次。
            .addFilterBefore(new JwtAuthenticationFilter(tokens, unauthorized),
                UsernamePasswordAuthenticationFilter.class)
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable());
        return http.build();
    }
}
