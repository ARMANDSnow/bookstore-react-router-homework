package com.homework.bookstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置（迭代三新增）。
 *
 * <h3>知识点 1：Spring Security 的工作模型 —— Servlet 过滤器链</h3>
 * <pre>
 * 浏览器请求 → Tomcat → [Security 过滤器链 FilterChainProxy] → DispatcherServlet → Controller
 *                        ↑ 认证(Authentication)、授权(Authorization)、CSRF 校验
 *                          都发生在请求进入 MVC 之前
 * </pre>
 * Spring Security 本质是一串 {@code jakarta.servlet.Filter}（十几个，按固定顺序排列），
 * 挡在 DispatcherServlet 前面。每个请求先经过它们，被拒绝的请求根本到不了 Controller。
 *
 * <h3>知识点 2：为什么"只加依赖不写配置"整个系统就 401 了？</h3>
 * Spring Boot 的自动配置（{@code SpringBootWebSecurityConfiguration}）发现 classpath 上有
 * Security 且容器里没有用户自定义的 {@link SecurityFilterChain} Bean 时，会注册一条
 * "默认链"：所有请求都要求认证（{@code anyRequest().authenticated()}），并随机生成一个
 * 登录密码打印在控制台。<b>一旦我们声明了自己的 SecurityFilterChain Bean，
 * 默认链就整体退位</b> —— 这就是 Spring Boot "约定优于配置 + 条件装配
 * （@ConditionalOnMissingBean）"思想的典型体现。
 *
 * <h3>知识点 3：本项目的"保守配置"策略（答辩可主动说明边界）</h3>
 * 本迭代引入 Security 的目标是<b>密码安全存储（BCrypt）</b>，而不是接口鉴权：
 * <ul>
 *   <li>登录仍走自研 {@code POST /api/v1/users/login}，登录态由前端 localStorage 保存，
 *       业务接口靠 {@code ?userId=} 传身份 —— 所以过滤器链对 API 全放行（permitAll）</li>
 *   <li>生产化路线（展望）：改用 Security 的认证体系签发 Session/JWT，
 *       过滤器链改为 {@code /api/**.authenticated()}，并用
 *       {@code @PreAuthorize} 做方法级权限控制</li>
 * </ul>
 */
@Configuration      // 声明这是一个配置类：Spring 容器启动时会扫描并执行其中的 @Bean 方法
@EnableWebSecurity  // 启用 Web 安全配置（Boot 下可省略，显式写出便于阅读）
public class SecurityConfig {

    /**
     * 密码编码器 Bean —— 全项目唯一的加密入口（IoC 容器单例）。
     *
     * <h3>知识点 4：BCrypt 为什么比 MD5/SHA-256 适合存密码？</h3>
     * <ul>
     *   <li><b>自带随机盐</b>：每次 encode 同一个明文，结果都不同
     *       （盐是随机的），数据库拖库后无法用"彩虹表"批量反查；
     *       而 MD5/SHA 是确定性函数，同一密码哈希值全库相同，一查一大片。</li>
     *   <li><b>故意慢（成本因子）</b>：BCrypt 内部迭代 2^strength 轮（默认 strength=10，
     *       即 1024 轮），单次校验毫秒级，暴力穷举的代价被指数放大；
     *       MD5/SHA 设计目标是"快"，反而利于攻击者。</li>
     *   <li><b>只能单向验证</b>：没有 decode 方法，校验用
     *       {@code matches(明文, 密文)} —— 对明文重新做一次哈希（用密文里存的盐）再比对。</li>
     * </ul>
     *
     * <h3>知识点 5：BCrypt 密文的结构（60 字符）</h3>
     * <pre>
     * $2a$10$N9qo8uLOickgx2ZMRZoMye.IjZAgcfl7p92ldGxad68LJZdL17lhW
     * └┬┘└┬┘└──────────┬─────────┘└────────────┬───────────────┘
     * 算法 成本   22 字符 Base64 盐          31 字符哈希结果
     * ($2a) (2^10轮)  （随机生成，明文存在密文里）
     * </pre>
     * 盐直接存在密文中，所以 matches() 不需要额外的盐字段 —— users 表只要一列 password。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 返回接口类型 PasswordEncoder 而不是实现类 BCryptPasswordEncoder：
        // 面向接口编程 —— 未来换 Argon2/SCrypt 只改这一行，注入方无感知（与 Service 接口分离同理）
        return new BCryptPasswordEncoder();
    }

    /**
     * 自定义安全过滤器链：替换 Spring Boot 的"全部拦截"默认链。
     *
     * <h3>知识点 6：为什么要 csrf.disable()？</h3>
     * CSRF（跨站请求伪造）防护默认开启：所有"写"请求（POST/PUT/DELETE）必须携带
     * 服务端签发的 CSRF Token，否则直接 403。CSRF 攻击的前提是"浏览器自动携带 Cookie 会话"，
     * 而本项目是纯 JSON API、不用 Cookie 维持登录态（前端 localStorage + 显式传参），
     * 不存在被伪造的会话，所以关闭它 —— 否则前端所有 POST 请求都会被 403 拦下。
     *
     * <h3>知识点 7：HttpSecurity 的链式 DSL</h3>
     * 每个方法（csrf/authorizeHttpRequests/...）配置一个安全维度，
     * 参数是 Lambda（Customizer 函数式接口），最后 build() 出过滤器链对象交给容器。
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 使用 WebConfig 中的 CORS 规则，允许本地开发端口变化时仍可访问 API。
            .cors(cors -> {})
            // 纯 JSON API + 无 Cookie 会话 → 关闭 CSRF（理由见上方 Javadoc 知识点 6）
            .csrf(csrf -> csrf.disable())
            // 授权规则：按声明顺序逐条匹配，第一条命中即生效
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").permitAll()   // 业务 API 全放行（本迭代不做接口鉴权）
                .anyRequest().permitAll())                // 兜底放行 /error、静态资源等
            // 关闭两种用不到的登录方式（否则未认证请求会被重定向到 Security 自带登录页）
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable());
        // 注意：这里不调 .cors()。现有 WebConfig 的 CORS 配置在 MVC 层（CorsRegistry），
        // 由于本链对所有请求 permitAll，浏览器的 OPTIONS 预检请求能穿过 Security 过滤器链
        // 到达 MVC 层被正常处理，无需在 Security 层重复配置 CorsConfigurationSource。
        return http.build();
    }
}
