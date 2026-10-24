package com.homework.bookstore.controller;

import com.homework.bookstore.dto.ApiResponse;
import com.homework.bookstore.dto.LoginRequest;
import com.homework.bookstore.security.JwtTokenService;
import com.homework.bookstore.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class TokenController {
    private final UserService users;
    private final JwtTokenService tokens;
    public TokenController(UserService users, JwtTokenService tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    // 【作业4 · 得分点4.3：登录并签发Token的HTTP入口】
    // POST携带用户名/密码，经参数校验和UserService的BCrypt比对后才调用issue。
    // BCrypt是带盐的单向密码哈希，登录时比对而非解密；JWT签名与密码哈希是两件事。
    // 返回accessToken、Bearer类型和有效期，客户端后续每次请求主动携带；这里不创建Session。
    // 答辩可先演示错误密码401，再成功登录；展示响应时隐藏完整accessToken。
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        var user = users.login(request); // 复用 BCrypt 密码和用户启用状态校验。
        return ApiResponse.success(new TokenResponse(tokens.issue(user), "Bearer", tokens.ttlSeconds()));
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
}
