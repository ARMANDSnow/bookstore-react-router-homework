/**
 * 在线书城后端工程（Spring Boot 3.3.5 + Spring Data JPA + MySQL 9）。
 *
 * <h2>分层架构总览（评分标准 D.ii）</h2>
 * <pre>
 *   Controller   ← HTTP 入口；@RestController；解析参数、校验、返回 ApiResponse
 *       ↓
 *   Service      ← 业务接口（{@link com.homework.bookstore.service}）
 *       ↓
 *   ServiceImpl  ← 业务实现，@Transactional 事务边界
 *       ↓
 *   Repository   ← Spring Data JPA 接口，运行时 JDK 动态代理生成实现
 *       ↓
 *   Entity       ← @Entity 映射 MySQL 表
 * </pre>
 *
 * <p>横向支撑包：
 * <ul>
 *   <li>{@code dto}    —— 接口的输入输出对象，与 Entity 解耦（避免泄露 password 等敏感字段）</li>
 *   <li>{@code config} —— 跨切面配置，如 CORS</li>
 *   <li>{@code service.exception} —— 业务异常体系</li>
 * </ul>
 *
 * <h2>启动入口</h2>
 * {@link com.homework.bookstore.BookstoreBackendApplication}
 *
 * <h2>对外端口</h2>
 * HTTP 8080，所有业务接口前缀 {@code /api/v1}
 *
 * <h2>关键文档</h2>
 * 详见 {@code backend/ARCHITECTURE.md}（目录走查 + 链路解析 + 答辩重点）。
 */
package com.homework.bookstore;
