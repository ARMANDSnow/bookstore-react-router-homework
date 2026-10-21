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
        var unauthorized = new JsonAuthenticationEntryPoint(mapper);
        http.cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
            .requestCache(cache -> cache.disable())
            .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                .requestMatchers("/api/secure/**").authenticated()
                .anyRequest().permitAll())
            // 直接加入 Security 链，避免注册为 Servlet Bean 后执行两次。
            .addFilterBefore(new JwtAuthenticationFilter(tokens, unauthorized),
                UsernamePasswordAuthenticationFilter.class)
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable());
        return http.build();
    }
}
