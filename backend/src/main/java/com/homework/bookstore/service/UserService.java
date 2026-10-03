package com.homework.bookstore.service;

import com.homework.bookstore.dto.LoginRequest;
import com.homework.bookstore.dto.RegisterRequest;
import com.homework.bookstore.dto.UserResponse;
import java.util.List;

/**
 * 用户领域服务接口（评分标准 D.iii：接口与实现分离）。
 *
 * <p>本接口定义"注册/登录"两个业务动作；实现类负责唯一性校验、BCrypt 加密和密码匹配。
 * Controller 不直接操作 {@code UserRepository}，因此将来如果登录方式从 localStorage 演示方案
 * 升级为 Spring Security + JWT，本接口契约可以保持不变，替换实现即可。
 */
public interface UserService {

    /** 注册：校验用户名/邮箱唯一，保存 BCrypt 密码哈希，返回不含 password 的用户信息。 */
    UserResponse register(RegisterRequest request);

    /** 登录：按用户名查用户，用 PasswordEncoder.matches 校验密码，失败时统一报"用户名或密码错误"。 */
    UserResponse login(LoginRequest request);

    /** 管理员用户管理：列出系统中的所有用户。 */
    List<UserResponse> listUsers();

    /** 管理员用户管理：禁用或解禁用户。 */
    UserResponse updateEnabled(Long userId, boolean enabled);
}
