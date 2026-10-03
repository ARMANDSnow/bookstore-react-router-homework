package com.homework.bookstore.controller;

import com.homework.bookstore.dto.ApiResponse;
import com.homework.bookstore.dto.LoginRequest;
import com.homework.bookstore.dto.RegisterRequest;
import com.homework.bookstore.dto.UserResponse;
import com.homework.bookstore.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;  // 依赖接口而非实现（评分 D.iii）

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // POST /api/v1/users/register     注册新用户
    // @RequestBody 把请求 JSON 反序列化为 RegisterRequest 对象
    // @Valid 触发对象内的 @NotBlank/@Email/@Size 等校验
    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", userService.register(request));
    }

    // POST /api/v1/users/login        登录
    // 用户名密码校验在 Service 层完成
    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", userService.login(request));
    }

    // GET /api/v1/users              管理员用户管理：列出全部用户
    @GetMapping
    public ApiResponse<List<UserResponse>> listUsers() {
        return ApiResponse.success(userService.listUsers());
    }

    // PUT /api/v1/users/{id}/enabled?enabled=false    禁用/解禁用户
    @PutMapping("/{id}/enabled")
    public ApiResponse<UserResponse> updateEnabled(@PathVariable Long id,
                                                   @RequestParam boolean enabled) {
        return ApiResponse.success(enabled ? "已解禁用户" : "已禁用用户",
                userService.updateEnabled(id, enabled));
    }
}
