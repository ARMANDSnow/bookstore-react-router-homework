package com.homework.bookstore.dto;

import com.homework.bookstore.entity.User;
import com.homework.bookstore.entity.UserRole;
import java.time.LocalDateTime;

/**
 * 用户响应 DTO。
 *
 * <p>刻意不包含 {@code password} 字段。即使数据库里存的是 BCrypt 密文，也不应该返回给前端；
 * DTO 层的一个重要作用就是屏蔽实体中的敏感字段。
 */
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String role;
    private Boolean enabled;
    private LocalDateTime createdAt;

    /** User 实体 → 对外响应；这里只拷贝允许公开的字段。 */
    public static UserResponse from(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        UserRole role = user.getRole() == null ? UserRole.CUSTOMER : user.getRole();
        response.setRole(role.name());
        response.setEnabled(!Boolean.FALSE.equals(user.getEnabled()));
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
