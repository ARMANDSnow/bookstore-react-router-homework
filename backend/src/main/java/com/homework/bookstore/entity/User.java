package com.homework.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id                                                  // 主键
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // MySQL AUTO_INCREMENT
    private Long id;

    // 用户名：唯一索引（unique = true）→ 数据库层面禁止重复
    // 同时 UserServiceImpl.register 里也用 existsByUsername 提前校验给出友好错
    @Column(nullable = false, unique = true, length = 60)
    private String username;

    // 密码：迭代三起存 BCrypt 加盐哈希（形如 $2a$10$...，固定 60 字符，length=120 足够）。
    // 加密/比对逻辑在 UserServiceImpl（encode/matches），加密器 Bean 在 config/SecurityConfig
    @Column(nullable = false, length = 120)
    private String password;

    // 邮箱：也加 unique 防止一个邮箱注册多个账号
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(length = 30)
    private String phone;  // 选填

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role;  // CUSTOMER / ADMIN

    @Column
    private Boolean enabled;  // false 表示账号被管理员禁用

    @Column(nullable = false)
    private LocalDateTime createdAt;  // 注册时间，由 @PrePersist 自动填

    // JPA 生命周期回调：在 INSERT 前自动给 createdAt 填当前时间
    // 这样业务代码 new User() 时不用关心时间戳
    @PrePersist
    public void prePersist() {
        if (role == null) {
            role = UserRole.CUSTOMER;
        }
        if (enabled == null) {
            enabled = true;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
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
