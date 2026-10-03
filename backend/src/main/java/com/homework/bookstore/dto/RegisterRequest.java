package com.homework.bookstore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求体。
 *
 * <p>这里的校验属于"接口输入格式"校验，例如不能为空、邮箱格式、长度上限；
 * "用户名是否已存在"属于业务规则，放在 {@code UserServiceImpl.register} 中查数据库完成。
 */
public class RegisterRequest {

    // 与 users.username 的 @Column(length = 60) 对齐，避免通过接口写入超长数据。
    @NotBlank(message = "用户名不能为空")
    @Size(max = 60, message = "用户名不能超过 60 个字符")
    private String username;

    // 这里限制的是"用户输入的明文长度"；入库前会被 BCrypt encode 成约 60 字符密文。
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 120, message = "密码长度应为 6 到 120 个字符")
    private String password;

    // 兼容老测试/接口调用：字段本身不强制非空；如果前端传了，就在 Service 中校验必须与 password 一致。
    private String confirmPassword;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 120, message = "邮箱不能超过 120 个字符")
    private String email;

    // 选填字段不加 @NotBlank；只限制最大长度。
    @Size(max = 30, message = "手机号不能超过 30 个字符")
    private String phone;

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

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
