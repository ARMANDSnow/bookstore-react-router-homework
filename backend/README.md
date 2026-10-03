# 知页书城后端

本目录是作业 1 迭代 2 的 Spring Boot 后端工程，提供用户注册和书籍查询接口。

## 运行准备

1. 创建 MySQL 数据库并导入脚本：

   ```sql
   source database/bookstore.sql;
   ```

2. 确认 `src/main/resources/application.yml` 中的 MySQL 连接信息正确。默认配置为：

   ```text
   数据库：bookstore
   用户名：root
   密码：（空，本机默认；有密码的环境用 MYSQL_PASSWORD 环境变量覆盖）
   端口：3306
   ```

## 启动

```bash
mvn spring-boot:run
```

服务启动后默认监听 `http://localhost:8080`。

## 测试

```bash
mvn test   # 26 个单元测试（Service 层 Mockito + Repository 层 @DataJpaTest，用 H2 内存库，无需 MySQL）
```

## API

- `POST /api/v1/users/register`：注册用户（密码 BCrypt 加密入库）。
- `POST /api/v1/users/login`：登录（BCrypt matches 比对）。
- `GET /api/v1/books`：查询全部书籍。
- `GET /api/v1/books?keyword=xxx`：按标题/作者关键字模糊搜索（迭代三新增）。
- `GET /api/v1/book/{id}`：查询单本书籍详情。
- `GET/POST/PUT/DELETE /api/v1/cart*`：购物车查询/加购/改数量/删除/清空。
- `POST /api/v1/orders?userId=`：下单（商品小计 + 运费、事务、价格快照、级联写明细、清空购物车）。
- `GET /api/v1/orders?userId=` / `GET /api/v1/orders/{id}`：订单列表 / 详情。

下单金额由后端计算：商品小计大于零且不足 99 元时收 12 元运费，满 99 元或零元免运费。历史订单保持已保存的金额；消费汇总包含订单运费，图书销售额按明细价格快照计算、不含运费。

更多：架构走查见 [ARCHITECTURE.md](ARCHITECTURE.md)。
