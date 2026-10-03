package com.homework.bookstore.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.homework.bookstore.dto.LoginRequest;
import com.homework.bookstore.dto.RegisterRequest;
import com.homework.bookstore.dto.UserResponse;
import com.homework.bookstore.entity.User;
import com.homework.bookstore.repository.UserRepository;
import com.homework.bookstore.service.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * {@link UserServiceImpl} 的单元测试 —— 注册防重、BCrypt 加密入库、登录比对。
 *
 * <h3>知识点 1：什么是"单元"测试？</h3>
 * 只测 UserServiceImpl 这一个类的业务逻辑，它的依赖（UserRepository）用 Mockito
 * 造的<b>假对象（mock）</b>替代 —— 不连数据库、不启动 Spring 容器，毫秒级跑完。
 * 对照：{@code BookRepositoryTest} 用 @DataJpaTest 连真实(内存)数据库，那属于切片/集成测试。
 *
 * <h3>知识点 2：Mockito 三件套</h3>
 * <ul>
 *   <li>{@code @ExtendWith(MockitoExtension.class)}：JUnit5 的扩展机制，
 *       让 JUnit 在跑测试前初始化所有 @Mock 字段</li>
 *   <li>{@code @Mock}：用 ByteBuddy 在运行期动态生成 UserRepository 接口的"假实现"，
 *       所有方法默认返回 null/空集合/false，行为由 when(...).thenReturn(...) 摆拍</li>
 *   <li>{@code verify(...)}：事后断言"某个方法（没）被调用过"，验证交互行为</li>
 * </ul>
 *
 * <h3>知识点 3：为什么 encoder 用真的、repository 用假的？（mock 的边界）</h3>
 * mock 的对象应该是"有外部依赖、慢、不可控"的东西（数据库、网络）。
 * BCryptPasswordEncoder 是纯内存算法，用真的既快又能顺带验证"存进库的确实是能
 * matches 回原文的密文"——如果把它也 mock 掉，加密逻辑就完全没被测到。
 * 构造时传成本因子 4（默认 10）：测试里把 2^10 轮降到 2^4 轮，提速约 64 倍，
 * 算法正确性不受影响。
 *
 * <h3>知识点 4：构造器注入 → 单测不需要 Spring</h3>
 * 下面 @BeforeEach 里直接 {@code new UserServiceImpl(mock仓库, 真encoder)} ——
 * 这正是"依赖注入让代码可测试"的实证：依赖从构造器传入而不是类内部 new 出来，
 * 测试想换成什么实现都行。若当初写成 @Autowired 字段注入，就只能靠反射注入了。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    /**
     * data.sql 里 demo 用户的密码哈希（BCrypt("123456")）。
     * 见下方 dataSqlDemoPasswordHashIsValid：用测试"守护"种子数据的正确性。
     */
    private static final String DATA_SQL_DEMO_HASH =
            "$2a$10$KxEpa.B.KXa2jPqIVfNc2.o4UGEmLgIKh25lvwuMk2BqytkfHQSw2";

    @Mock
    private UserRepository userRepository;  // 假的数据访问层：不连库，行为全靠摆拍

    private PasswordEncoder passwordEncoder;  // 真的加密器（理由见类 Javadoc 知识点 3）
    private UserServiceImpl userService;      // 被测对象（SUT, System Under Test）

    @BeforeEach
    void setUp() {
        // 每个测试方法执行前都会重新走一遍：保证测试之间互不污染（JUnit5 生命周期）
        passwordEncoder = new BCryptPasswordEncoder(4);  // 成本因子 4，测试提速
        userService = new UserServiceImpl(userRepository, passwordEncoder);
    }

    // 小工具：造一个入参对象
    private RegisterRequest registerRequest(String username, String password, String email) {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setPassword(password);
        req.setEmail(email);
        return req;
    }

    @Test
    @DisplayName("注册成功：入库密码必须是 BCrypt 密文且能 matches 回原文")
    void registerEncryptsPassword() {
        // ---------- given（摆拍依赖行为）----------
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        // save 的摆拍：模拟数据库行为——回传实体并补上自增主键
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(100L);
            return u;
        });

        // ---------- when（执行被测方法）----------
        UserResponse resp = userService.register(registerRequest("alice", "123456", "alice@test.com"));

        // ---------- then（断言结果 + 交互）----------
        assertEquals(100L, resp.getId());
        assertEquals("alice", resp.getUsername());

        // ArgumentCaptor：把传给 save() 的实参"抓"出来检查——
        // 这是验证"进数据库前发生了什么加工"的标准手法
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        String storedPassword = captor.getValue().getPassword();

        assertTrue(storedPassword.startsWith("$2a$"), "存库的必须是 BCrypt 密文而非明文");
        assertTrue(passwordEncoder.matches("123456", storedPassword), "密文必须能验证回原文");
    }

    @Test
    @DisplayName("注册重名：抛 40901 且绝不触发 save")
    void registerRejectsDuplicateUsername() {
        // given：用户名已存在（短路返回，后面的邮箱查重根本不会执行，所以不摆拍它）
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        // when + then：assertThrows 断言"必须抛出这个异常"，并拿到异常对象继续断言错误码
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.register(registerRequest("alice", "123456", "alice@test.com")));
        assertEquals(40901, ex.getCode());

        // verify + never：防御性断言——查重失败时绝不能有任何写库动作
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("登录成功：明文经 matches 与库中密文比对通过")
    void loginSucceedsWithCorrectPassword() {
        // given：库里的用户存的是密文（模拟注册时 encode 过）
        User stored = new User();
        stored.setId(1L);
        stored.setUsername("demo");
        stored.setPassword(passwordEncoder.encode("123456"));
        stored.setEmail("demo@bookstore.com");
        when(userRepository.findByUsername("demo")).thenReturn(Optional.of(stored));

        LoginRequest req = new LoginRequest();
        req.setUsername("demo");
        req.setPassword("123456");

        // when
        UserResponse resp = userService.login(req);

        // then：登录成功返回用户信息（且 UserResponse 里没有 password 字段——DTO 屏蔽敏感数据）
        assertEquals("demo", resp.getUsername());
        assertEquals(1L, resp.getId());
    }

    @Test
    @DisplayName("登录失败-密码错误：抛 40101")
    void loginRejectsWrongPassword() {
        User stored = new User();
        stored.setUsername("demo");
        stored.setPassword(passwordEncoder.encode("123456"));
        when(userRepository.findByUsername("demo")).thenReturn(Optional.of(stored));

        LoginRequest req = new LoginRequest();
        req.setUsername("demo");
        req.setPassword("wrong-password");

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(req));
        assertEquals(40101, ex.getCode());
    }

    @Test
    @DisplayName("登录失败-用户不存在：同样抛 40101（防用户名枚举）")
    void loginRejectsUnknownUser() {
        // given：查无此人
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setUsername("ghost");
        req.setPassword("123456");

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(req));
        // "用户不存在"和"密码错误"故意用同一个错误码/文案：
        // 如果区分开，攻击者就能靠试用户名 + 观察报错差异，扫出系统里有哪些账号
        assertEquals(40101, ex.getCode());
    }

    @Test
    @DisplayName("守护测试：data.sql 里 demo 用户的哈希必须能验证 123456")
    void dataSqlDemoPasswordHashIsValid() {
        // 把种子数据的正确性纳入测试保护：谁误改了 data.sql 里的哈希串，
        // mvn test 立刻红——答辩演示账号 demo/123456 永远登得上
        assertTrue(new BCryptPasswordEncoder().matches("123456", DATA_SQL_DEMO_HASH),
                "data.sql 中 demo 用户的 BCrypt 哈希与明文 123456 不匹配，请重新生成");
    }
}
