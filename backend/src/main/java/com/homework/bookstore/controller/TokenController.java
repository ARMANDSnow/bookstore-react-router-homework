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

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        var user = users.login(request); // 复用 BCrypt 密码和用户启用状态校验。
        return ApiResponse.success(new TokenResponse(tokens.issue(user), "Bearer", tokens.ttlSeconds()));
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
}
