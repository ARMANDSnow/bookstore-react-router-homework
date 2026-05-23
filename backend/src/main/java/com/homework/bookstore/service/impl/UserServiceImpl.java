package com.homework.bookstore.service.impl;

import com.homework.bookstore.dto.LoginRequest;
import com.homework.bookstore.dto.RegisterRequest;
import com.homework.bookstore.dto.UserResponse;
import com.homework.bookstore.entity.User;
import com.homework.bookstore.repository.UserRepository;
import com.homework.bookstore.service.UserService;
import com.homework.bookstore.service.exception.BusinessException;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(40901, "用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(40902, "邮箱已注册");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public UserResponse login(LoginRequest request) {
        Optional<User> opt = userRepository.findByUsername(request.getUsername());
        if (opt.isEmpty() || !opt.get().getPassword().equals(request.getPassword())) {
            throw new BusinessException(40101, "用户名或密码错误");
        }
        return UserResponse.from(opt.get());
    }
}
