# 知页书城 · 迭代三

> 互联网应用开发技术课程作业
> 技术栈：**React 19 + React Router 7 + Ant Design 6 + Vite 7**（前端） · **Spring Boot 3.3.5 + Spring Data JPA + Spring Security + MySQL 8**（后端） · **Fetch API**（前后端通信） · **JUnit 5 + Mockito**（测试）
>
> **答辩演示账号**：`demo` / `123456`　|　**一键跑测试**：`cd backend && mvn test`（26 用例）

---

## 目录

- [一、迭代进展](#一迭代进展)
- [二、目录结构与设计理由](#二目录结构与设计理由)
- [三、快速开始](#三快速开始)
- [四、数据库设计](#四数据库设计)
- [五、REST 接口文档](#五rest-接口文档)
- [六、核心链路详解：下订单](#六核心链路详解下订单)
- [七、架构与分层说明](#七架构与分层说明)
- [八、评分标准自查表](#八评分标准自查表)
- [九、联调脚本（curl 冒烟）](#九联调脚本curl-冒烟)

---

## 一、迭代进展

### 迭代二（前后端集成）

> 在迭代一基础上，把"前端展示的所有数据都来自后端数据库、前端的所有操作都反映回数据库"这条主线打通。

1. **后端**：新增登录、购物车、订单 三个领域；表结构、实体、Repository、Service（接口 + 实现）、Controller 一应俱全。
2. **前端**：登录态接入、购物车数据源切到后端、下单调真接口、订单历史页。
3. **前后端通信**：使用 Fetch API；后端所有响应都用统一 `ApiResponse<T>` 包装；前端 `request()` 统一解包。
4. **数据库**：所有持久化由 Spring Data JPA 负责，定制 SQL 用 `@Query` 展示。

### 迭代三（架构优化 + 答辩准备）

1. **Spring Security + BCrypt**：密码不再明文——注册时 `encode()` 加盐哈希、登录时 `matches()` 比对；`SecurityConfig` 保守放行 `/api/**`，不影响既有功能。
2. **图书搜索**：`GET /api/v1/books?keyword=` 按标题/作者模糊查询（派生查询 + JPQL 两种写法对照）；前端列表页新增搜索框，与分类过滤叠加。
3. **JUnit 单元测试**：`backend/src/test/` 下 26 个用例（Service 层 Mockito + Repository 层 `@DataJpaTest`），`mvn test` 全绿，用 H2 内存库、不依赖 MySQL。
4. **详情页接真接口**：`BookDetailPage` 改为 `useEffect` 调 `GET /api/v1/book/{id}`，优先展示数据库数据。

### 界面改造（温暖纸感）

采用米白背景、墨绿主色和宋体标题，通过 CSS 变量与 Ant Design `ConfigProvider` 统一颜色、字号、按钮和表单。

- **导航与选书**：顶部品牌、全站搜索与横向导航；书籍封面完整显示，分类和价格排序配合真实搜索结果。
- **购买流程**：详情页分区展示书籍信息；购物车在桌面采用商品列表与右侧金额摘要，手机改为逐件商品布局；订单保留日期与书名筛选。
- **账户与管理**：登录注册、个人信息、消费统计、书籍管理和用户管理沿用同一套样式，保留现有接口和操作。
- **手机适配**：导航抽屉、自动换行的筛选与按钮、完整可操作的表单，以及可横向滚动的数据表格。

---

## 二、目录结构与设计理由

```
线上书城系统/
├── README.md                ← 本文档
├── package.json             ← 前端依赖与脚本
├── vite.config.js           ← Vite 代理 /api → :8080
├── index.html               ← Vite 入口
├── public/                  ← 静态资源
├── src/                     ← 前端源代码
│   ├── main.jsx
│   ├── App.jsx              ← 路由 + 全局状态（用户态、购物车）
│   ├── api/
│   │   └── bookstoreApi.js  ← Fetch HTTP 封装：解包 ApiResponse
│   ├── services/            ← 业务服务层
│   │   ├── authService.js
│   │   └── cartService.js
│   ├── components/          ← 通用组件
│   │   ├── Layout.jsx       ← 顶部搜索与导航、账户、手机导航抽屉
│   │   ├── BookCard.jsx
│   │   ├── HeroBanner.jsx
│   │   └── CategoryFilter.jsx
│   ├── pages/               ← 路由级页面
│   │   ├── BookListPage.jsx
│   │   ├── BookDetailPage.jsx
│   │   ├── CartPage.jsx
│   │   └── ProfilePage.jsx  ← 登录 / 注册 / 订单历史
│   ├── utils/               ← 纯函数工具
│   │   ├── auth.js          ← localStorage 用户态读写
│   │   └── formatter.js     ← formatPrice / formatDateTime
│   ├── data/Data.json       ← 离线降级数据（后端不可达时用）
│   └── styles.css
└── backend/                 ← Spring Boot 后端
    ├── pom.xml
    ├── database/
    │   └── bookstore.sql    ← 完整 DDL + 种子数据
    ├── src/main/
        ├── java/com/homework/bookstore/
        │   ├── BookstoreBackendApplication.java
        │   ├── controller/             ← 控制层：HTTP 入口
        │   │   ├── BookController.java
        │   │   ├── UserController.java
        │   │   ├── CartController.java
        │   │   ├── OrderController.java
        │   │   └── GlobalExceptionHandler.java
        │   ├── service/                ← 服务层：业务编排（接口）
        │   │   ├── UserService.java
        │   │   ├── CartService.java
        │   │   ├── OrderService.java
        │   │   ├── exception/BusinessException.java
        │   │   └── impl/               ← 服务层实现
        │   │       ├── UserServiceImpl.java
        │   │       ├── CartServiceImpl.java
        │   │       └── OrderServiceImpl.java
        │   ├── repository/             ← 数据访问层
        │   │   ├── BookRepository.java
        │   │   ├── UserRepository.java
        │   │   ├── CartItemRepository.java
        │   │   ├── OrderRepository.java
        │   │   └── OrderItemRepository.java
        │   ├── entity/                 ← 实体层（@Entity）
        │   │   ├── Book.java
        │   │   ├── User.java
        │   │   ├── CartItem.java
        │   │   ├── Order.java
        │   │   ├── OrderItem.java
        │   │   └── OrderStatus.java
        │   ├── dto/                    ← 出入参对象
        │   │   ├── ApiResponse.java
        │   │   ├── LoginRequest.java
        │   │   ├── RegisterRequest.java
        │   │   ├── UserResponse.java
        │   │   ├── AddToCartRequest.java
        │   │   ├── UpdateCartItemRequest.java
        │   │   ├── CartItemDto.java
        │   │   ├── OrderDto.java
        │   │   └── OrderItemDto.java
        │   └── config/
        │       ├── WebConfig.java      ← CORS 跨域配置
        │       └── SecurityConfig.java ← 迭代三：Spring Security 过滤器链 + BCrypt 编码器
        └── resources/
            ├── application.yml         ← 数据源 + JPA 配置
            ├── application-local.yml
            └── data.sql                ← 启动时自动执行的种子数据（demo 用户为 BCrypt 密文）
    └── src/test/                       ← 迭代三新增：JUnit 单元测试
        ├── java/com/homework/bookstore/service/impl/  ← 4 个 Service 测试类（Mockito 单测）
        ├── java/com/homework/bookstore/repository/    ← BookRepositoryTest（@DataJpaTest 切片）
        └── resources/application.yml                  ← 测试专用 H2 内存库配置
```

### 前端目录设计理由

| 目录 | 职责 | 设计理由（对应评分 D.i） |
|------|------|--------------------------|
| `components/` | UI 组件，不含业务逻辑 | 复用与可测试 |
| `pages/` | 路由级页面，承载交互逻辑 | 与 React Router 1:1 对应 |
| `api/` | 纯 HTTP 封装 | 把"发请求"与"做业务"解耦：换接口前缀只改一处 |
| `services/` | 业务封装层 | 隐藏 fetch 细节、保留扩展空间（缓存、防抖、本地与远程合并） |
| `utils/` | 纯函数工具 | 与组件解耦，便于单测 |
| `data/Data.json` | 离线降级 | 后端不可达时仍能演示前端 UI |

### 后端目录设计理由

| 层 | 包名 | 职责 | 设计理由（对应评分 D.ii / D.iii） |
|----|------|------|-----------------------------------|
| 控制层 | `controller/` | 解析 HTTP 请求、调 service、包装 `ApiResponse` | 只关心 HTTP，不做业务 |
| 服务层 | `service/` (接口) + `service/impl/` (实现) | 业务编排、事务 | **接口与实现分离**：单测好做、可换实现，对应评分 D.iii |
| 数据访问层 | `repository/` | 通过 JPA 与数据库交互 | 隔离持久化技术细节 |
| 实体层 | `entity/` | `@Entity` 映射数据库表 | 评分标准 A.iii："抽象成实体类再做处理" |
| DTO | `dto/` | 出入参对象 | 隔离实体与外部契约，避免脏字段（如 password）泄露 |

---

## 三、快速开始与操作说明

### 0. 环境要求

| 工具 | 版本 | 验证命令 |
|------|------|----------|
| JDK | 17 及以上（编译目标为 17，实测 17–26 均可运行） | `java -version` |
| Maven | 3.8+（或用 IDE 内置） | `mvn -version` |
| Node.js | 20.19+（20.x）或 22.12+ | `node --version` |
| MySQL | 8.x（服务需已启动） | `mysqladmin ping` |

> **关于 JDK 版本**：`pom.xml` 编译目标是 Java 17。若你的机器是 JDK 21+，跑测试所需的 Mockito / ByteBuddy 版本已在 `pom.xml` 里处理好兼容，`mvn test` 可直接运行，无需切换 JDK。

### 1. 准备数据库（先启动 MySQL）

后端默认连接 `localhost:3306/bookstore`，用户名 `root`，**密码默认留空**。若你的 MySQL root 设了密码，用环境变量覆盖（见第 2 步），**不必改任何代码**。启动顺序固定为：**MySQL → 后端 → 前端 → 浏览器操作**。

建库有两种等价方式，任选其一：

- **方式 A（推荐，零操作）**：什么都不用做。后端首次启动时 JPA 的 `ddl-auto: update` 会自动建库建表，`data.sql` 自动插入 6 本书 + demo 用户。连接串带了 `createDatabaseIfNotExist=true`，库不存在也会自动创建。
- **方式 B（手动导入 DDL）**：
  ```bash
  mysql -uroot < backend/database/bookstore.sql          # root 无密码
  mysql -uroot -p < backend/database/bookstore.sql       # root 有密码（回车后输入）
  ```

### 2. 启动后端（端口 8080，保持终端不要关闭）

```bash
cd backend
mvn spring-boot:run                        # ① root 无密码，直接启动
MYSQL_PASSWORD=你的密码 mvn spring-boot:run   # ② root 有密码，用环境变量覆盖（不改文件）
```

可覆盖的环境变量（均有默认值）：`MYSQL_HOST`(localhost)、`MYSQL_PORT`(3306)、`MYSQL_DATABASE`(bookstore)、`MYSQL_USER`(root)、`MYSQL_PASSWORD`(空)。

看到日志 `Started BookstoreBackendApplication` 即启动成功。这个终端需要一直开着，前端请求会通过 Vite 代理转到这里。快速自检：

```bash
curl localhost:8080/api/v1/books                 # 应返回 6 本书的 JSON
curl 'localhost:8080/api/v1/books?keyword=代码'  # 迭代三搜索：应只返回《代码整洁之道》
```

### 3. 启动前端（端口 5173，另开一个终端）

```bash
# 注意：在项目根目录执行，不是 backend/
npm install       # 首次运行需要；已装过可跳过
npm run dev
```

浏览器打开 <http://localhost:5173>（或终端提示的实际地址）。Vite 已配代理，`/api` 请求自动转发到 8080，开发时无跨域问题。前端终端也需要保持运行。

### 4. 运行单元测试（迭代三）

```bash
cd backend
mvn test          # 26 个用例：Service 层 Mockito 单测 + Repository 层 @DataJpaTest 切片
```

测试用 **H2 内存数据库**，不需要 MySQL、不污染开发库。控制台会打印 Hibernate 生成的真实 SQL（已开 `org.hibernate.SQL: debug`），可现场演示派生查询/JPQL 翻译结果。

### 5. 打生产包（可选）

```bash
npm run build                        # 前端产物 → dist/
cd backend && mvn clean package      # 后端产物 → backend/target/*.jar，可 java -jar 运行
```

### 6. 浏览器操作流程（验收演示推荐顺序）

1. 主页 `/books`：来自数据库的 6 本书；顶部**搜索框**输入「代码」或「norman」（迭代三新增，作者名忽略大小写）→ 结果实时过滤，可与分类 Tab 叠加；
2. `/profile`：用 `demo / 123456` 登录（密码在库中是 **BCrypt 密文**，登录时 `matches` 比对）；
3. 进任意书详情页（数据来自 `GET /api/v1/book/{id}` 真实接口，F12 Network 可见）→「加入购物车」；
4. `/cart`：改数量、删除；点「提交订单」→ 自动跳 `/profile` 看到新订单；
5. **持久化校验**：退出登录再登录，订单仍在，购物车（若没下单）仍在。

---

## 四、数据库设计

### 4.1 ER 关系（文字版）

```
users (1) ─── (N) cart_items (N) ─── (1) books
users (1) ─── (N) orders     (1) ─── (N) order_items (N) ─── (1) books
```

### 4.2 表字段

#### users — 用户表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| username | VARCHAR(60) | NOT NULL, UNIQUE | 登录名 |
| password | VARCHAR(120) | NOT NULL | 密码（迭代三起为 BCrypt 加盐哈希，`$2a$10$...` 60 字符密文） |
| email | VARCHAR(120) | NOT NULL, UNIQUE | 邮箱 |
| phone | VARCHAR(30) | NULL | 手机号 |
| created_at | DATETIME | NOT NULL | 注册时间 |

#### books — 书籍表

主键 `id` 是字符串（如 `clean-code`、`three-body`），其余字段为标题、作者、价格、分类、图片、富文本简介等共 16 列。

#### cart_items — 购物车明细

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | 主键 |
| user_id | BIGINT | NOT NULL, FK→users | 所属用户 |
| book_id | VARCHAR(80) | NOT NULL, FK→books | 书籍 |
| quantity | INT | NOT NULL, default 1 | 数量 |
| created_at | DATETIME | NOT NULL | |
| updated_at | DATETIME | NOT NULL | |
| UK | (user_id, book_id) | UNIQUE | 同一用户同一本书只有一条记录，再次加车数量累加 |

#### orders — 订单主表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | |
| user_id | BIGINT | NOT NULL, FK→users | 下单用户 |
| total_amount | DECIMAL(10,2) | NOT NULL | 订单总额 |
| status | VARCHAR(20) | NOT NULL | `PENDING` / `PAID` |
| created_at | DATETIME | NOT NULL | |

#### order_items — 订单明细

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK | |
| order_id | BIGINT | NOT NULL, FK→orders ON DELETE CASCADE | |
| book_id | VARCHAR(80) | NOT NULL, FK→books | |
| book_title | VARCHAR(120) | NOT NULL | **快照**：避免书名后续被改影响历史订单 |
| book_image | VARCHAR(255) | NULL | 同上，封面快照 |
| unit_price | DECIMAL(10,2) | NOT NULL | **快照**：成交价 |
| quantity | INT | NOT NULL | 数量 |

> **为什么订单明细要存快照？** 因为书的价格、书名以后可能改，如果订单不存快照、用户三年后回头看订单，会发现金额对不上当时支付的款。

### 4.3 JPA 与数据库的连接 / 持久化全过程

1. **建立连接**：`application.yml` 配置 datasource URL / username / password；Spring Boot 启动时通过 `HikariCP` 创建连接池，懒加载第一次访问数据库时才建立物理连接。
2. **DDL**：`spring.jpa.hibernate.ddl-auto=update` 让 Hibernate 在启动时扫描所有 `@Entity` 类，若数据库里没有对应表则建表、字段缺失则加列（**不会**删字段）。
3. **种子数据**：`spring.sql.init.mode=always` + `defer-datasource-initialization=true` 让 `data.sql` 在 JPA 建完表后执行，幂等地插入 books 和 demo 用户。
4. **请求时**：Controller → Service（带 `@Transactional`）→ Repository 方法 → JPA 生成 JPQL/SQL → JDBC 执行 → 结果通过 Hibernate 反射映射回实体对象。
5. **事务**：以 `OrderServiceImpl.placeOrder` 为例，整个方法在一个事务里，期间任何一步抛异常都会触发回滚，保证"订单已生成但购物车没清空"这种坏数据不会出现。

---

## 五、REST 接口文档

所有接口返回体统一为：

```json
{
  "code": 0,                // 0 = 成功；非 0 = 业务错误
  "message": "ok",
  "data": { ... }           // 业务数据
}
```

> 评分标准 C.i：**JSON 数据格式设计合理**——使用统一外壳后，前端只需写一段 `unwrap()` 即可处理所有响应，不用为每个接口单独判错。

### 5.1 书籍

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/books` | 列出所有书籍 |
| GET | `/api/v1/books?keyword={关键字}` | 按书名或作者模糊搜索，忽略大小写 |
| GET | `/api/v1/book/{id}` | 获取书籍详情 |

**示例**：

```
GET /api/v1/books
```
```json
{
  "code": 0,
  "message": "ok",
  "data": [
    { "id": "clean-code", "title": "代码整洁之道", "price": 79.00, ... }
  ]
}
```

### 5.2 用户

| 方法 | 路径 | 请求体 | 说明 |
|------|------|--------|------|
| POST | `/api/v1/users/register` | `{username, password, email, phone}` | 注册 |
| POST | `/api/v1/users/login` | `{username, password}` | 登录 |

```
POST /api/v1/users/login
Body: { "username": "demo", "password": "123456" }
```
```json
{
  "code": 0,
  "message": "登录成功",
  "data": { "id": 1, "username": "demo", "email": "demo@bookstore.com", "phone": "13800000000", "createdAt": "2026-05-23T22:00:00" }
}
```

### 5.3 购物车

| 方法 | 路径 | 请求 | 说明 |
|------|------|------|------|
| GET | `/api/v1/cart?userId={id}` | — | 查询用户购物车 |
| POST | `/api/v1/cart/items` | `{userId, bookId, quantity}` | 加入购物车（已存在则累加） |
| PUT | `/api/v1/cart/items/{id}` | `{quantity}` | 修改某条数量 |
| DELETE | `/api/v1/cart/items/{id}` | — | 删除某条 |
| DELETE | `/api/v1/cart?userId={id}` | — | 清空 |

```
GET /api/v1/cart?userId=1
```
```json
{
  "code": 0,
  "data": [
    {
      "id": 8,
      "bookId": "clean-code",
      "title": "代码整洁之道",
      "image": "/images/代码整洁之道.JPG",
      "price": 79.00,
      "originalPrice": 99.00,
      "quantity": 2,
      "subtotal": 158.00
    }
  ]
}
```

### 5.4 订单

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/orders?userId={id}` | 下单（事务：读购物车 → 生成订单 → 清空购物车） |
| GET | `/api/v1/orders?userId={id}` | 用户订单列表 |
| GET | `/api/v1/orders/{id}` | 订单详情 |

```
POST /api/v1/orders?userId=1
```
```json
{
  "code": 0,
  "message": "下单成功",
  "data": {
    "id": 7,
    "userId": 1,
    "totalAmount": 158.00,
    "status": "PAID",
    "createdAt": "2026-05-23T23:00:00",
    "items": [ { "bookId": "clean-code", "bookTitle": "代码整洁之道", "unitPrice": 79.00, "quantity": 2, "subtotal": 158.00 } ]
  }
}
```

---

## 六、核心链路详解：下订单

> **对应评分标准 C.ii**：详述"前端 JS 发请求 → 后端 Java 处理 → 访问数据库 → 抽象组装数据 → 发回前端 → 前端刷新"全过程。
> 选最长的一条链路：**用户在购物车点"提交订单"**。

### Step 1 · 前端发出请求

[src/pages/CartPage.jsx](src/pages/CartPage.jsx) 的"提交订单"按钮 `onClick={onSubmitOrder}` 触发 [src/App.jsx](src/App.jsx) 中的 `handleSubmitOrder()`：

```jsx
async function handleSubmitOrder() {
  if (!user) { message.warning("请先登录"); return false; }
  if (!cart.length) return false;
  try {
    await checkout(user.id);          // ← cartService.checkout
    await refreshCart();
    message.success("结算成功！已生成订单。");
    navigate("/profile");
    return true;
  } catch (err) {
    message.error(err.message || "下单失败");
    return false;
  }
}
```

`checkout` 来自 [src/services/cartService.js](src/services/cartService.js)，最终调到 [src/api/bookstoreApi.js](src/api/bookstoreApi.js)：

```js
export function placeOrder(userId) {
  return request(`/orders?userId=${encodeURIComponent(userId)}`, {
    method: "POST",
  });
}
```

`request()` 用 `fetch` 发送：

```js
const response = await fetch(`/api/v1${path}`, {
  headers: { "Content-Type": "application/json" },
  method: "POST",
});
```

Vite dev server 看到 `/api` 前缀，按 [vite.config.js](vite.config.js) 的 proxy 转发到 `http://localhost:8080`。

### Step 2 · 后端 Controller 接收

[OrderController.placeOrder](backend/src/main/java/com/homework/bookstore/controller/OrderController.java)：

```java
@PostMapping
public ApiResponse<OrderDto> placeOrder(@RequestParam Long userId) {
    return ApiResponse.success("下单成功", orderService.placeOrder(userId));
}
```

Controller 只做一件事：把 HTTP 参数交给 Service，并用 `ApiResponse` 包装返回。

### Step 3 · Service 编排业务逻辑（事务）

[OrderServiceImpl.placeOrder](backend/src/main/java/com/homework/bookstore/service/impl/OrderServiceImpl.java)：

```java
private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("99.00");
private static final BigDecimal SHIPPING_FEE = new BigDecimal("12.00");

@Override
@Transactional                       // 整体事务
public OrderDto placeOrder(Long userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(40401, "用户不存在"));

    List<CartItem> cartItems = cartItemRepository
            .findByUser_IdOrderByCreatedAtAsc(userId);     // ① 读购物车
    if (cartItems.isEmpty()) {
        throw new BusinessException(40001, "购物车为空，无法下单");
    }

    Order order = new Order();
    order.setUser(user);
    order.setStatus(OrderStatus.PAID);

    BigDecimal subtotal = BigDecimal.ZERO;
    for (CartItem cartItem : cartItems) {                  // ② 构建明细 + 累计商品小计
        OrderItem oi = new OrderItem();
        oi.setBook(cartItem.getBook());
        oi.setBookTitle(cartItem.getBook().getTitle());    // 价格、书名快照
        oi.setBookImage(cartItem.getBook().getImage());
        oi.setUnitPrice(cartItem.getBook().getPrice());
        oi.setQuantity(cartItem.getQuantity());
        order.addItem(oi);
        subtotal = subtotal.add(cartItem.getBook().getPrice()
                .multiply(BigDecimal.valueOf(cartItem.getQuantity())));
    }
    BigDecimal shipping = subtotal.signum() > 0
            && subtotal.compareTo(FREE_SHIPPING_THRESHOLD) < 0
            ? SHIPPING_FEE : BigDecimal.ZERO;
    order.setTotalAmount(subtotal.add(shipping));

    Order saved = orderRepository.save(order);             // ③ 落库（含级联保存 items）
    cartItemRepository.deleteByUserId(userId);             // ④ 清空购物车
    return OrderDto.from(saved);                           // ⑤ 抽象成 DTO
}
```

事务保证：①~④ 任何一步失败，整个事务回滚，不会出现"订单已建但购物车没清"或"购物车清了但订单丢失"。

订单应付总额为商品小计加运费：商品小计大于零且不足 99 元时收 12 元，满 99 元或零元免运费。例如三体 49 元 × 2 本，商品小计 98 元，应付并保存 110 元。规则由后端在下单事务中计算，不接受前端传入的金额。历史订单保留已保存的总额；个人总金额与用户消费榜按订单总额汇总，图书销售额和购书明细金额按商品价格快照汇总，不含运费。

### Step 4 · Repository 实际访问数据库

`orderRepository.save(order)` 由 Spring Data JPA 实现：
- Hibernate 通过反射读取 `@Entity` 注解，生成 SQL `INSERT INTO orders ...`
- 因为 `Order` 的 `items` 是 `@OneToMany(cascade=ALL)`，会自动 `INSERT INTO order_items ...` N 行
- `cartItemRepository.deleteByUserId(userId)` 走的是 [CartItemRepository](backend/src/main/java/com/homework/bookstore/repository/CartItemRepository.java) 里的 `@Query("delete from CartItem c where c.user.id = :userId")` 自定义 JPQL，对应一次 `DELETE FROM cart_items WHERE user_id = ?`

### Step 5 · 抽象数据再发回

`OrderDto.from(saved)` 把 JPA 实体 `Order` 转成对外的 `OrderDto`，剥离 `password / 双向引用` 等内部字段。Jackson 自动序列化为 JSON。最终经过 Controller 包成：

```json
{ "code": 0, "message": "下单成功", "data": { "id": 7, "totalAmount": 158.00, ... } }
```

### Step 6 · 前端解包 + 刷新视图

回到 [bookstoreApi.js](src/api/bookstoreApi.js) 的 `request()`：

```js
if (payload && Object.prototype.hasOwnProperty.call(payload, "code")) {
  if (payload.code !== 0) throw new Error(payload.message || "...");
  return payload.data;          // 拆掉外壳，只返回 data
}
```

[App.jsx](src/App.jsx) 的 `handleSubmitOrder` 拿到结果后：
1. `await refreshCart()` 重新拉一次购物车（已是空数组）；
2. `message.success(...)` 顶部弹提示；
3. `navigate("/profile")` 跳到个人中心；
4. [ProfilePage.jsx](src/pages/ProfilePage.jsx) 中的 `<OrderHistory />` 在 `useEffect` 里调 `getOrders(user.id)`，新订单出现在列表顶部。

整个链路前端 5 个文件、后端 6 个类协同完成，没有一处使用本地缓存数据。

---

## 七、架构与分层说明

### 7.1 后端三大原则

1. **关注点分离**：每层只对相邻层负责。Controller 不直接碰 Repository，Service 不直接碰 HTTP。
2. **接口与实现分离**（评分 D.iii）：每个 Service 都写成 `interface XxxService` + `XxxServiceImpl`，调用方依赖接口；以后想换实现（如换成调用第三方支付）只换实现类即可，调用方零修改。
3. **实体抽象**（评分 A.iii）：数据库表 → `@Entity`；前端契约 → DTO。中间转换由 `XxxDto.from(entity)` 显式做。

### 7.2 前端三大原则

1. **HTTP 与业务解耦**：`api/` 只负责发请求，`services/` 包装业务，组件不直接 fetch。
2. **状态最小化**：用户态只存在 localStorage + App.jsx 顶层 state；购物车只在登录态变化时拉取，避免重复请求。
3. **离线降级**：书籍列表后端不可达时回落到 `data/Data.json`，不让作业演示在断网时直接白屏。

### 7.3 跨域处理

[backend/src/main/java/com/homework/bookstore/config/WebConfig.java](backend/src/main/java/com/homework/bookstore/config/WebConfig.java) 通过 `addCorsMappings` 允许 `http://127.0.0.1:5173` 与 `http://localhost:5173`。开发时 Vite 走 proxy 直接转发，浏览器看到的同源是 5173，不会触发 CORS；但保留 CORS 配置便于在生产把前端独立部署。

---

## 八、迭代三评分标准自查表

本表按《课程大作业迭代3要求细则.pdf》的 30 分口径整理，方便验收前逐项核对。

| 评分项 | 分值 | 实现情况 | 关键位置 |
|--------|-----:|:--------:|----------|
| **A. 功能实现** | **10** | 已实现 | 登录 / 注册、书籍列表、书籍详情、搜索、购物车、下单、订单历史均走后端真实接口 |
| 登录与注册 | — | 已实现 | [UserController](backend/src/main/java/com/homework/bookstore/controller/UserController.java)、[UserServiceImpl](backend/src/main/java/com/homework/bookstore/service/impl/UserServiceImpl.java)、[ProfilePage](src/pages/ProfilePage.jsx) |
| 书籍列表、详情、搜索 | — | 已实现 | [BookController](backend/src/main/java/com/homework/bookstore/controller/BookController.java)、[BookServiceImpl](backend/src/main/java/com/homework/bookstore/service/impl/BookServiceImpl.java)、[BookListPage](src/pages/BookListPage.jsx)、[BookDetailPage](src/pages/BookDetailPage.jsx) |
| 购物车与订单 | — | 已实现 | [CartController](backend/src/main/java/com/homework/bookstore/controller/CartController.java)、[OrderController](backend/src/main/java/com/homework/bookstore/controller/OrderController.java)、[CartPage](src/pages/CartPage.jsx) |
| **B. 技术方案** | **10** | 已实现 | 前端组件化、后端分层、接口与实现分离、Spring JPA / ORM |
| 前端组件化开发 | 4 | 已实现 | `components/`、`services/`、`pages/`、`utils/` 分包；HTTP 封装在 [bookstoreApi.js](src/api/bookstoreApi.js) |
| 后端分层架构 | 4 | 已实现 | `controller/`、`service/`、`service/impl/`、`repository/`、`entity/`、`dto/`；详见第 2 节与第 7 节 |
| 接口与实现分离 + 依赖注入 | — | 已实现 | `BookService` / `BookServiceImpl`、`UserService` / `UserServiceImpl`、`CartService` / `CartServiceImpl`、`OrderService` / `OrderServiceImpl`，通过构造器注入 Repository |
| ORM / Spring JPA | 2 | 已实现 | [entity/](backend/src/main/java/com/homework/bookstore/entity/) 与 [repository/](backend/src/main/java/com/homework/bookstore/repository/)；`Order` 到 `OrderItem` 使用级联保存 |
| **C. 代码质量** | **5** | 已实现 | 命名、分层、封装、测试与必要注释 |
| 项目结构、命名、封装、测试 | 3 | 已实现 | 前后端目录清晰；后端 [src/test](backend/src/test/) 含 26 个 JUnit / Mockito / DataJpaTest 用例 |
| 必要注释 | 2 | 已实现 | `pom.xml`、配置类、关键业务方法、README 与架构文档补充了答辩说明 |
| **D. 界面友好** | **5** | 已实现 | Ant Design 页面、电子商务常见操作路径、响应式布局 |
| 操作习惯 | 2 | 已实现 | 书籍浏览 → 详情 → 加购 → 购物车 → 下单 → 个人中心订单历史 |
| 界面美观 | 2 | 已实现 | 米白与墨绿主题、完整书封、分类筛选、详情分区、响应式购物车、订单卡片及统一账户与管理页面 |
| 体验完整度 | 1 | 已实现 | 登录态持久化、后端不可达时图书列表降级展示、操作反馈使用 Ant Design message |

---

## 九、联调脚本（curl 冒烟）

后端启动后，按顺序执行可一次性验证主链路：

```bash
# 1. 登录
curl -X POST localhost:8080/api/v1/users/login \
     -H 'Content-Type: application/json' \
     -d '{"username":"demo","password":"123456"}'
# → 返回 { code:0, data:{ id:1, username:"demo", ... } }
# 记下 data.id，下面用 USER_ID 代替

# 2. 查书籍列表
curl localhost:8080/api/v1/books

# 3. 加入购物车（demo userId=1）
curl -X POST localhost:8080/api/v1/cart/items \
     -H 'Content-Type: application/json' \
     -d '{"userId":1,"bookId":"clean-code","quantity":2}'

# 4. 再加另一本
curl -X POST localhost:8080/api/v1/cart/items \
     -H 'Content-Type: application/json' \
     -d '{"userId":1,"bookId":"three-body","quantity":1}'

# 5. 查购物车
curl "localhost:8080/api/v1/cart?userId=1"

# 6. 下单
curl -X POST "localhost:8080/api/v1/orders?userId=1"

# 7. 查订单
curl "localhost:8080/api/v1/orders?userId=1"

# 8. 再查购物车，应为空
curl "localhost:8080/api/v1/cart?userId=1"
```

MySQL 端校验：

```sql
USE bookstore;
SELECT * FROM cart_items;     -- 下单后应为空
SELECT * FROM orders;         -- 至少 1 行
SELECT * FROM order_items;    -- 至少 2 行
```

---

## 附录：提交清单与 zip 打包

按迭代三细则第 2 节要求，提交内容如下（**前端不含 `node_modules`，后端不含 `lib`/`target`**）：

**前端**：`src/`、`public/`（含书籍封面图）、`index.html`、`package.json`、`package-lock.json`、`vite.config.js`
**后端**：`backend/src/`（含 `main` 与 `test`）、`backend/pom.xml`、`backend/database/bookstore.sql`、`backend/postman/`
**文档**：本 `README.md`、`backend/ARCHITECTURE.md`

一键打包为 zip（在项目根目录执行，只挑该交的文件，天然排除依赖与构建产物）：

```bash
zip -r 524031910745-作业5.zip \
  src public index.html package.json package-lock.json vite.config.js \
  README.md \
  backend/src backend/pom.xml backend/database backend/postman backend/README.md backend/ARCHITECTURE.md \
  -x '*/node_modules/*' '*/target/*' '*/dist/*' '*/.DS_Store'
```

打包后可用下面命令检查压缩包内容，确认没有 `node_modules/`、`backend/target/`、`dist/`：

```bash
unzip -l 524031910745-作业5.zip | grep -E 'node_modules|backend/target|(^|/)dist/' || echo "OK: 未包含依赖和构建产物"
```
