# 知页书城 · 迭代二

> 互联网应用开发技术课程作业
> 技术栈：**React 19 + React Router 7 + Ant Design 6 + Vite 7**（前端） · **Spring Boot 3.3.5 + Spring Data JPA + MySQL 8**（后端） · **Fetch API**（前后端通信）

---

## 目录

- [一、迭代二要做的事](#一迭代二要做的事)
- [二、目录结构与设计理由](#二目录结构与设计理由)
- [三、快速开始](#三快速开始)
- [四、数据库设计](#四数据库设计)
- [五、REST 接口文档](#五rest-接口文档)
- [六、核心链路详解：下订单](#六核心链路详解下订单)
- [七、架构与分层说明](#七架构与分层说明)
- [八、评分标准自查表](#八评分标准自查表)
- [九、联调脚本（curl 冒烟）](#九联调脚本curl-冒烟)

---

## 一、迭代二要做的事

> 在迭代一基础上，把"前端展示的所有数据都来自后端数据库、前端的所有操作都反映回数据库"这条主线打通。

具体落地：
1. **后端**：新增登录、购物车、订单 三个领域；表结构、实体、Repository、Service（接口 + 实现）、Controller 一应俱全。
2. **前端**：登录态接入、购物车数据源切到后端、下单调真接口、订单历史页。
3. **前后端通信**：使用 Fetch API；后端所有响应都用统一 `ApiResponse<T>` 包装；前端 `request()` 统一解包。
4. **数据库**：所有持久化由 Spring Data JPA 负责，定制 SQL 用 `@Query` 写原生 SQL 展示。

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
│   │   ├── Layout.jsx       ← Sider + Header（用户名、退出按钮）
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
    └── src/main/
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
        │       └── WebConfig.java      ← CORS 跨域配置
        └── resources/
            ├── application.yml         ← 数据源 + JPA 配置
            ├── application-local.yml
            └── data.sql                ← 启动时自动执行的种子数据
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

## 三、快速开始

### 0. 前置依赖

| 工具 | 版本 |
|------|------|
| Node.js | ≥ 18 |
| JDK | 17 |
| MySQL | 8.x |
| Maven | 3.8+（或使用 IDE 内置） |

### 1. 准备数据库

```bash
# 启动 MySQL（按你的安装方式）；用 root/123456 或自行修改 application.yml
mysql -uroot -p < backend/database/bookstore.sql
```

`bookstore.sql` 会：
1. 创建 `bookstore` 数据库（utf8mb4）；
2. 创建 `users / books / cart_items / orders / order_items` 五张表；
3. 插入 6 本示例书籍与 1 个 demo 用户（用户名 `demo`、密码 `123456`）。

> 第一次启动也可以**不导入 SQL**：JPA 的 `ddl-auto: update` 会根据实体类自动建表，`data.sql` 会自动 INSERT 种子数据。两者效果等价，区别只是是否手动跑一次 DDL。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

监听 `http://localhost:8080`。

### 3. 启动前端

```bash
# 项目根目录
npm install
npm run dev
```

打开 `http://127.0.0.1:5173`。

### 4. 体验

1. 访问主页 `/books`，看到来自数据库的 6 本书；
2. 进 `/profile`，用 `demo / 123456` 登录，右上角出现欢迎语；
3. 进任意书详情，点"加入购物车"；
4. 进 `/cart`，看到购物车里有书，可改数量、删除；
5. 点"提交订单"，自动跳回 `/profile` 看到刚生成的订单；
6. **持久化校验**：退出登录再登录，订单仍在，购物车（若没下单）仍在。

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
| password | VARCHAR(120) | NOT NULL | 密码（作业演示明文） |
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

    BigDecimal total = BigDecimal.ZERO;
    for (CartItem cartItem : cartItems) {                  // ② 构建明细 + 累计总价
        OrderItem oi = new OrderItem();
        oi.setBook(cartItem.getBook());
        oi.setBookTitle(cartItem.getBook().getTitle());    // 价格、书名快照
        oi.setBookImage(cartItem.getBook().getImage());
        oi.setUnitPrice(cartItem.getBook().getPrice());
        oi.setQuantity(cartItem.getQuantity());
        order.addItem(oi);
        total = total.add(cartItem.getBook().getPrice()
                .multiply(BigDecimal.valueOf(cartItem.getQuantity())));
    }
    order.setTotalAmount(total);

    Order saved = orderRepository.save(order);             // ③ 落库（含级联保存 items）
    cartItemRepository.deleteByUserId(userId);             // ④ 清空购物车
    return OrderDto.from(saved);                           // ⑤ 抽象成 DTO
}
```

事务保证：①~④ 任何一步失败，整个事务回滚，不会出现"订单已建但购物车没清"或"购物车清了但订单丢失"。

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

## 八、评分标准自查表

| 评分项 | 分值 | 是否实现 | 关键文件 |
|--------|-----:|:--------:|----------|
| **A. 数据库访问（5 分）** | | | |
| A.i 正确连接并访问数据库，能讲清持久化过程 | 2 | ✅ | [application.yml](backend/src/main/resources/application.yml)、本 README 第 4.3 节 |
| A.ii 正确使用 Repository 方法 / 定制持久化 | 2 | ✅ | 派生方法：[CartItemRepository](backend/src/main/java/com/homework/bookstore/repository/CartItemRepository.java) `findByUser_IdOrderByCreatedAtAsc`；定制 `@Query`：[OrderRepository](backend/src/main/java/com/homework/bookstore/repository/OrderRepository.java) `sumTotalAmountByUserId`、[CartItemRepository](backend/src/main/java/com/homework/bookstore/repository/CartItemRepository.java) `deleteByUserId` |
| A.iii 数据抽象为实体类 | 1 | ✅ | [entity/](backend/src/main/java/com/homework/bookstore/entity/) 下 6 个 `@Entity` |
| **B. 功能（5 分）** | | | |
| B.i.1 登录（数据库中的用户名/密码） | 1 | ✅ | [POST /api/v1/users/login](backend/src/main/java/com/homework/bookstore/controller/UserController.java) + [ProfilePage LoginPanel](src/pages/ProfilePage.jsx) |
| B.i.2 书籍列表主页 | 1 | ✅ | [GET /api/v1/books](backend/src/main/java/com/homework/bookstore/controller/BookController.java) + [BookListPage](src/pages/BookListPage.jsx) |
| B.i.3 书籍详情 | 1 | ✅ | [GET /api/v1/book/{id}](backend/src/main/java/com/homework/bookstore/controller/BookController.java) + [BookDetailPage](src/pages/BookDetailPage.jsx) |
| B.i.4 加入购物车 | 1 | ✅ | [POST /api/v1/cart/items](backend/src/main/java/com/homework/bookstore/controller/CartController.java)，**存数据库** |
| B.i.5 下订单 | 1 | ✅ | [POST /api/v1/orders](backend/src/main/java/com/homework/bookstore/controller/OrderController.java) + 事务：[OrderServiceImpl](backend/src/main/java/com/homework/bookstore/service/impl/OrderServiceImpl.java) |
| B.ii 页面联动、数据来自数据库 | — | ✅ | 加书 → 购物车列表立刻看到；下单 → 订单页立刻看到（详见第六章链路） |
| **C. 前后端集成（5 分）** | | | |
| C.i 异步 Fetch、JSON 格式合理 | 2 | ✅ | 统一外壳 `ApiResponse<T>`（[ApiResponse.java](backend/src/main/java/com/homework/bookstore/dto/ApiResponse.java)）；前端统一解包（[bookstoreApi.js](src/api/bookstoreApi.js) `request()`） |
| C.ii 详述全链路 | 3 | ✅ | 本 README 第 6 章 |
| **D. 系统架构（5 分）** | | | |
| D.i 前端目录合理并解释 | 2 | ✅ | 第 2 节"前端目录设计理由" |
| D.ii 后端分层架构 | 2 | ✅ | controller / service / repository / entity / dto / config 六层（第 2 节后端目录） |
| D.iii 接口与实现分离 | 1 | ✅ | `UserService`+`UserServiceImpl`、`CartService`+`CartServiceImpl`、`OrderService`+`OrderServiceImpl` 三对 |

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

## 附录：提交清单

按课程要求提交以下内容：

- ✅ React 工程源代码（**不要打包 `node_modules`**，可执行 `rm -rf node_modules`）
- ✅ Spring Boot 工程源代码（**不要打包 `target`、`lib`**，可执行 `rm -rf backend/target`）
- ✅ 数据库 SQL 脚本：`backend/database/bookstore.sql`
- ✅ 本 README
