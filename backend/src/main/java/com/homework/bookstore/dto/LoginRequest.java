package com.homework.bookstore.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求体。
 *
 * <p>只接收明文密码用于本次校验；后端不会保存明文，{@code UserServiceImpl.login}
 * 会调用 {@code PasswordEncoder.matches(明文, 数据库中的 BCrypt 密文)}。
 */
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
