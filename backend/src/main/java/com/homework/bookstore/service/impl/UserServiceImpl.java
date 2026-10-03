package com.homework.bookstore.service.impl;

import com.homework.bookstore.dto.LoginRequest;
import com.homework.bookstore.dto.RegisterRequest;
import com.homework.bookstore.dto.UserResponse;
import com.homework.bookstore.entity.User;
import com.homework.bookstore.entity.UserRole;
import com.homework.bookstore.repository.UserRepository;
import com.homework.bookstore.service.UserService;
import com.homework.bookstore.service.exception.BusinessException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户业务实现：注册（防重 + BCrypt 加密）+ 登录（BCrypt 密码比对）。
 *
 * <h3>迭代三升级：密码从明文存储改为 BCrypt 加盐哈希</h3>
 * <ul>
 *   <li><b>注册</b>：{@code passwordEncoder.encode(明文)} → 存入库的是 60 字符的
 *       {@code $2a$10$...} 密文（盐随机，同一密码每次密文都不同）</li>
 *   <li><b>登录</b>：{@code passwordEncoder.matches(明文, 库中密文)} —— 从密文里取出盐，
 *       对用户输入重做一次哈希再比对；<b>不是解密</b>（BCrypt 无法解密）</li>
 *   <li>encoder 的选型与原理详见 {@link com.homework.bookstore.config.SecurityConfig}</li>
 * </ul>
 *
 * <h3>仍保留的教学简化（答辩可主动提）</h3>
 * <ul>
 *   <li><b>无 Session / JWT</b>：登录成功直接返回用户信息，由前端写到 localStorage；
 *       后续业务接口靠 {@code ?userId=} 传身份。生产环境应用 Spring Security
 *       的认证体系签发凭证并做接口鉴权（SecurityConfig 中已留展望说明）</li>
 * </ul>
 *
 * <h3>错误码约定</h3>
 * <ul>
 *   <li>{@code 40101} 用户名或密码错误（登录）</li>
 *   <li>{@code 40901} 用户名已存在（注册）</li>
 *   <li>{@code 40902} 邮箱已注册（注册）</li>
 * </ul>
 * 由 {@link com.homework.bookstore.controller.GlobalExceptionHandler} 统一翻译为 HTTP 状态码。
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;    // 数据访问依赖：用户表
    private final PasswordEncoder passwordEncoder;  // 加密依赖：SecurityConfig 里声明的 BCrypt Bean

    // 构造器注入（Spring 推荐方式）：
    // 1) 字段可以声明为 final —— 对象一旦构造完成依赖就不可变，线程安全
    // 2) 缺依赖时启动直接报错（fail-fast），而不是运行到一半 NPE
    // 3) 单元测试可以直接 new UserServiceImpl(mockRepo, realEncoder)，不需要启动 Spring 容器
    //    （见 src/test 下 UserServiceImplTest —— 这正是"依赖注入让代码可测试"的实证）
    // Spring 4.3+ 起单构造器可省略 @Autowired，容器会自动按类型从 IoC 容器中找 Bean 传进来
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.getConfirmPassword() != null
                && !Objects.equals(request.getPassword(), request.getConfirmPassword())) {
            throw new BusinessException(40002, "两次输入的密码不一致");
        }
        // 【1】用户名查重 —— 派生方法 existsByUsername
        // SQL: select count(*) from users where username = ?
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(40901, "用户名已存在");
        }
        // 【2】邮箱查重
        // SQL: select count(*) from users where email = ?
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(40902, "邮箱已注册");
        }
        // 【3】构造 User 实体（id 此时为 null，触发 INSERT）
        User user = new User();
        user.setUsername(request.getUsername());
        // 迭代三：入库前用 BCrypt 加盐哈希。encode() 每次都随机生成盐，
        // 所以两个用户即使密码相同，库里的密文也完全不同（防彩虹表批量破解）
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(resolveRole(request.getUsername()));
        user.setEnabled(true);

        // 【4】save —— 触发 @PrePersist 自动填写 createdAt
        // SQL: insert into users (username, password, email, phone, created_at) values (?,?,?,?,?)
        return UserResponse.from(userRepository.save(user));  // 实体 → DTO（不暴露 password）
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request) {
        // 按用户名查用户 —— 派生方法 findByUsername，返回 Optional 避免 null 风险
        // SQL: select * from users where username = ?
        Optional<User> opt = userRepository.findByUsername(request.getUsername());

        // 校验：用户不存在 或 密码不匹配 都报同一个错（避免暴露"用户存在与否"，防用户名枚举攻击）
        // 迭代三：明文 equals 比对 → BCrypt matches 比对。
        // matches(明文, 密文) 内部：从密文前 29 字符取出算法版本+成本因子+盐，
        // 用同样参数对明文重新哈希，再与密文后 31 字符恒定时间比较
        if (opt.isEmpty() || !passwordEncoder.matches(request.getPassword(), opt.get().getPassword())) {
            throw new BusinessException(40101, "用户名或密码错误");
        }
        if (Boolean.FALSE.equals(opt.get().getEnabled())) {
            throw new BusinessException(40301, "您的账号已经被禁用");
        }
        // 登录成功，返回用户信息（前端会写到 localStorage 作为登录态）
        return UserResponse.from(opt.get());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse updateEnabled(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在"));
        user.setEnabled(enabled);
        return UserResponse.from(userRepository.save(user));
    }

    private UserRole resolveRole(String username) {
        return "admin".equalsIgnoreCase(username) ? UserRole.ADMIN : UserRole.CUSTOMER;
    }
}
