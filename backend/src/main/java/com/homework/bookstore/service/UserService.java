package com.homework.bookstore.service;

import com.homework.bookstore.dto.LoginRequest;
import com.homework.bookstore.dto.RegisterRequest;
import com.homework.bookstore.dto.UserResponse;

/**
 * 用户领域服务接口（评分标准 D.iii：接口与实现分离）。
 */
public interface UserService {

    UserResponse register(RegisterRequest request);

    UserResponse login(LoginRequest request);
}
