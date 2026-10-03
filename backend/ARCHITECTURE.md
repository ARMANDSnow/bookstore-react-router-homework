# 后端架构走查（答辩复习用）

> 这是一份"打开 IDE 对着讲"的速查手册。每一节都直接对应代码里的 package 和类。
> 配套：每个 package 下都有 `package-info.java`，鼠标悬停可看包级文档。
>
> 迭代三新增：Spring Security + BCrypt、图书搜索接口、`src/test` 下 19 个单元测试。

---

## 一、整体架构图

```
浏览器 (5173, React + Antd)
        │
        │  fetch + JSON
        ▼
Vite proxy  /api/*  ──►  Spring Boot (8080)
                              │
                              ▼
                         ┌────────────┐
                         │ Controller │  ← @RestController，HTTP 入口
                         │            │     ApiResponse 统一包装
                         ├────────────┤
                         │  Service   │  ← 接口（D.iii 接口与实现分离）
                         │  + Impl    │     @Transactional 事务边界
                         ├────────────┤
                         │ Repository │  ← Spring Data JPA 接口
                         │            │     JDK 动态代理生成实现
                         ├────────────┤
                         │   Entity   │  ← @Entity 映射 MySQL 表
                         └────────────┘
                              │
                              │  JDBC + HikariCP 连接池
                              ▼
                         MySQL (3306)
```

---

## 二、目录树（鼠标对应到文件即可打开讲）

```
backend/
├── pom.xml                              ← Maven 依赖与构建
├── ARCHITECTURE.md                      ← 本文档
├── README.md                            ← 启动说明
├── database/bookstore.sql               ← 提交用建库脚本（5 表 + 6 书 + demo 用户）
└── src/main/
    ├── resources/
    │   ├── application.yml              ← 数据源 / JPA / Tomcat 配置
    │   └── data.sql                     ← Spring Boot 启动时自动 INSERT 种子数据
    └── java/com/homework/bookstore/
        ├── BookstoreBackendApplication  ← 启动类（main）
        ├── package-info.java            ← 工程总览（IDE 悬停可看）
        │
        ├── config/                      ◆ 横切配置
        │   ├── package-info.java
        │   └── WebConfig                ← CORS 跨域配置
        │
        ├── entity/                      ◆ 实体层 = 数据库表
        │   ├── package-info.java        ← 实体-表对照表
        │   ├── Book                     ←→ books
        │   ├── User                     ←→ users
        │   ├── CartItem                 ←→ cart_items（UNIQUE 约束 user_id + book_id）
        │   ├── Order                    ←→ orders   （OneToMany 级联到 OrderItem）
        │   ├── OrderItem                ←→ order_items（快照 book_title / unit_price）
        │   └── OrderStatus              ← 枚举 PENDING / PAID（@Enumerated STRING）
        │
        ├── repository/                  ◆ 数据访问层（JPA 接口）
        │   ├── package-info.java        ← 三种查询写法的总结
        │   ├── BookRepository           ← 只用基类方法
        │   ├── UserRepository           ← 派生方法 existsByUsername / findByUsername
        │   ├── CartItemRepository       ← @Modifying @Query 批量删除示范
        │   ├── OrderRepository          ← @Query nativeQuery=true 聚合 SQL 示范
        │   └── OrderItemRepository      ← 只用基类方法
        │
        ├── dto/                         ◆ 接口输入输出契约
        │   ├── package-info.java        ← DTO vs Entity 的区别
        │   ├── ApiResponse<T>           ← ★ 统一响应外壳 {code, message, data}
        │   ├── LoginRequest             ← @NotBlank 校验
        │   ├── RegisterRequest          ← @Email / @Size 校验
        │   ├── AddToCartRequest
        │   ├── UpdateCartItemRequest
        │   ├── UserResponse             ← 不含 password
        │   ├── CartItemDto              ← 含书籍快照
        │   ├── OrderDto
        │   └── OrderItemDto
        │
        ├── service/                     ◆ 业务接口（D.iii）
        │   ├── package-info.java
        │   ├── UserService              ← 注册 / 登录
        │   ├── CartService              ← 增删改查 / 清空
        │   ├── OrderService             ← 下单 / 查询
        │   ├── exception/
        │   │   └── BusinessException    ← 业务异常基类
        │   └── impl/                    业务实现
        │       ├── package-info.java
        │       ├── UserServiceImpl
        │       ├── CartServiceImpl      ← 加车「存在则累加」策略
        │       └── OrderServiceImpl     ← ★ 下单事务（核心方法）
        │
        └── controller/                  ◆ HTTP 入口
            ├── package-info.java        ← 所有端点速查
            ├── BookController           ← GET /books, /book/{id}
            ├── UserController           ← POST /users/register, /login
            ├── CartController           ← /cart 全套 CRUD
            ├── OrderController          ← /orders 下单 + 查询
            └── GlobalExceptionHandler   ← @RestControllerAdvice 全局异常翻译
```

---

## 三、各层职责（一句话版）

| 层 | 关键注解 | 干什么 | 不干什么 |
|---|---|---|---|
| Controller | `@RestController` | 收请求、解参数、调 Service、返响应 | 不写业务、不调 Repository |
| Service（接口） | — | 抽象业务契约 | 没有任何实现细节 |
| ServiceImpl | `@Service` `@Transactional` | 业务逻辑、事务边界、组合多 Repository | 不直接操作 HTTP |
| Repository | `JpaRepository` `@Query` | 读写数据库 | 不含业务判断 |
| Entity | `@Entity` `@Table` | 映射表，做 ORM | 不写持久化操作 |
| DTO | — | 接口输入输出对象 | 不参与持久化 |
| Config | `@Configuration` | 全局配置（CORS、连接池等） | 不掺业务 |

---

## 四、核心链路：「下订单」全过程（评分标准 C.ii，3 分）

> 答辩时打开 `OrderServiceImpl.placeOrder` 这一个方法，对照下面 10 步念，把"事务"、"快照"、"级联"三个词重点说出来。

```
1. CartPage.jsx
   用户点「提交订单」按钮，触发 onSubmitOrder()
        │
2. App.jsx handleSubmitOrder
   → cartService.checkout(userId)
   → bookstoreApi.placeOrder(userId)
        │
3. fetch 'POST /api/v1/orders?userId=1'   ─── 这里是异步、JSON、Promise
        │
4. Vite proxy 转发 5173 → 8080
        │
5. Spring Boot Tomcat 接收
   → OrderController.placeOrder(userId)
        │
6. orderService.placeOrder(userId)
   ┌─────────── @Transactional 事务边界 ────────────┐
   │  6.1  userRepository.findById(userId)         │  ← SELECT users
   │  6.2  cartItemRepository.findByUser_IdOrder...│  ← SELECT cart_items
   │  6.3  空购物车？抛 BusinessException(40001)    │
   │  6.4  new Order(), 遍历购物车 new OrderItem    │  ← 携带书名/单价快照
   │  6.5  orderRepository.save(order)             │  ← INSERT orders
   │                                               │      + 级联 INSERT order_items
   │  6.6  cartItemRepository.deleteByUserId(...)  │  ← DELETE cart_items
   └──────── 正常返回则提交，异常则全部回滚 ────────┘
        │
7. Service 返 OrderDto
        │
8. Controller 包成 ApiResponse.success(...)
   Jackson 序列化为 JSON
        │
9. fetch 收到 200 OK
   request() 解 code=0 → 返回 data
        │
10. App.jsx
    refreshCart() 重拉空购物车
    navigate('/profile')
    ProfilePage 的 OrderHistory 拉新订单展示
```

---

## 五、数据库连接是怎么建起来的（评分标准 A.i）

> 老师常问："你怎么连上数据库的？"完整答案：

1. **配置阶段**（`application.yml`）
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/bookstore?...
       username: root
       password: ${MYSQL_PASSWORD:123456}
       driver-class-name: com.mysql.cj.jdbc.Driver
   ```

2. **Spring Boot 启动**（`BookstoreBackendApplication.main`）
   - `@SpringBootApplication` 触发 `@EnableAutoConfiguration`
   - `DataSourceAutoConfiguration` 读到 `spring.datasource.*` 配置
   - 创建 **HikariCP 连接池**（默认 10 个连接），注入 Spring 容器

3. **JPA 自动配置**（`HibernateJpaAutoConfiguration`）
   - 创建 `EntityManagerFactory`，扫描所有 `@Entity` 类
   - 按 `ddl-auto: update` 比对表结构，缺字段就 ALTER

4. **业务调用时**
   - Repository 方法 → Hibernate `EntityManager.persist/find/...`
   - EntityManager 从 HikariCP **借连接**，发 SQL
   - MySQL 执行并返回结果
   - Hibernate 把结果集**映射回实体对象**
   - 用完连接还回池中

---

## 六、Repository 没有实现类怎么用？

Spring Data JPA 启动时扫描所有继承 `JpaRepository<T, ID>` 的接口，
通过 **JDK 动态代理**（`java.lang.reflect.Proxy`）在内存里生成代理对象注入容器。

调用方法时实际走代理：

```
你调 userRepository.findByUsername("demo")
      │
      ▼
JDK Proxy.invoke()
      │
      ▼
解析方法名 → "findBy" + 字段名 "Username" → 生成 JPQL:
   select u from User u where u.username = ?1
      │
      ▼
Hibernate 翻译成 SQL → 通过 HikariCP 连接发给 MySQL
      │
      ▼
ResultSet → 映射成 User 对象 → 包成 Optional 返回
```

---

## 七、三种 Repository 查询写法（评分标准 A.ii）

| 写法 | 例子 | 适用场景 |
|---|---|---|
| 派生方法 | `findByUsername(String)` | 简单条件查询 |
| `@Query` + JPQL | `delete from CartItem c where c.user.id = :userId` | 批量修改（配 `@Modifying`） |
| `@Query` + 原生 SQL | `SELECT COALESCE(SUM(...)) FROM orders WHERE ...` | 聚合统计 / 用方言特性 |

本项目三种都有，分别在：
- `UserRepository.findByUsername`
- `CartItemRepository.deleteByUserId`
- `OrderRepository.sumTotalAmountByUserId`

---

## 八、关键设计 Q&A 速查

| 问 | 答 |
|---|---|
| 为什么 OrderItem 要冗余存 book_title / unit_price ？ | 订单快照。书改名/改价不能影响历史订单 |
| 为什么 cart_items 要加 (user_id, book_id) 唯一约束？ | 同一用户同一本书只占一行；并发场景兜底 |
| 为什么不用 Spring Security / JWT ？ | 教学项目简化。生产应加 |
| 为什么密码明文？ | 同上。生产用 BCrypt |
| 为什么所有响应都包 ApiResponse ？ | 统一前端处理，给业务错误码一席之地 |
| 为什么 Service 拆接口+实现？ | 评分标准 D.iii；可替换、可测试 |
| 为什么 OrderServiceImpl 用构造器注入而不是 @Autowired 字段？ | 依赖不可变、启动期发现缺失、便于单测 |
| ddl-auto: update 上生产会怎样？ | 危险。生产用 validate 或 Flyway |

---

## 九、评分标准对照表

| 项 | 分 | 实现 | 文件 |
|---|---|---|---|
| A.i  连接 & 持久化过程 | 2 | application.yml + HikariCP + JPA | 本文档 §五 |
| A.ii 正确写 SQL/Repository | 2 | 派生 + @Query JPQL + @Query nativeQuery | OrderRepository, CartItemRepository |
| A.iii 数据抽象为实体类 | 1 | 5 个 @Entity | entity/ |
| B.i.1 登录 | 1 | POST /users/login | UserController + UserServiceImpl |
| B.i.2 书籍列表 | 1 | GET /books | BookController |
| B.i.3 书籍详情 | 1 | GET /book/{id} | BookController |
| B.i.4 加购物车（数据库） | 1 | POST /cart/items | CartController + CartServiceImpl |
| B.i.5 下订单 | 1 | POST /orders | OrderController + OrderServiceImpl |
| C.i  Fetch + JSON 格式合理 | 2 | ApiResponse 统一外壳 | dto/ApiResponse.java |
| C.ii 链路详解 | 3 | 见 §四 | OrderServiceImpl.placeOrder |
| D.i  前端结构合理 | 2 | components/pages/api/services/utils | 前端工程 |
| D.ii 后端分层合理 | 2 | controller/service+impl/repository/entity/dto/config | 本文档 §二 |
| D.iii 接口与实现分离 | 1 | UserService/CartService/OrderService 各有 Impl | service/ + service/impl/ |

---

## 十、答辩急救包

| 问什么 | 翻到哪 |
|---|---|
| 让讲下单事务 | `OrderServiceImpl.placeOrder` |
| 让看你写的 SQL | `OrderRepository` (@Query nativeQuery) |
| 让讲 Repository 怎么工作 | 本文档 §六 |
| 让讲 CORS | `WebConfig.java` 类注释 |
| 让讲为什么用 DTO | `dto/package-info.java` |
| 让讲启动流程 | `BookstoreBackendApplication` 类注释 |
| 让讲为什么分层 | 本文档 §三 |
| 让讲 ApiResponse | `dto/ApiResponse.java` 类注释 |
| 让讲事务原理 | `OrderServiceImpl` 类注释（AOP 代理那一段） |
