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
   密码：123456
   端口：3306
   ```

## 启动

```bash
mvn spring-boot:run
```

服务启动后默认监听 `http://localhost:8080`。

## API

- `POST /api/v1/users/register`：注册用户。
- `GET /api/v1/books`：查询全部书籍。
- `GET /api/v1/book/{id}`：查询单本书籍详情。
