/**
 * <h2>Entity 层 —— 实体类（持久化模型）</h2>
 *
 * <p>每个 {@code @Entity} 类对应数据库里的一张表（评分标准 A.iii「数据库中的数据抽象成实体类」）。
 * Hibernate 在启动时根据 {@code application.yml} 中 {@code spring.jpa.hibernate.ddl-auto: update} 配置，
 * 自动比对实体与数据库表结构差异，缺字段就 ALTER 加列（<b>不会删字段、不会删数据</b>）。
 *
 * <h3>本包成员（5 个实体 + 1 个枚举）</h3>
 * <table border="1" summary="实体-表对照">
 *   <tr><th>Java 类</th><th>MySQL 表</th><th>主键</th><th>关联</th></tr>
 *   <tr><td>{@link com.homework.bookstore.entity.Book}</td>      <td>books</td>       <td>String id</td><td>—</td></tr>
 *   <tr><td>{@link com.homework.bookstore.entity.User}</td>      <td>users</td>       <td>Long id</td>  <td>—</td></tr>
 *   <tr><td>{@link com.homework.bookstore.entity.CartItem}</td>  <td>cart_items</td>  <td>Long id</td>  <td>@ManyToOne → User / Book</td></tr>
 *   <tr><td>{@link com.homework.bookstore.entity.Order}</td>     <td>orders</td>      <td>Long id</td>  <td>@OneToMany → OrderItem</td></tr>
 *   <tr><td>{@link com.homework.bookstore.entity.OrderItem}</td> <td>order_items</td> <td>Long id</td>  <td>@ManyToOne → Order / Book</td></tr>
 *   <tr><td>{@link com.homework.bookstore.entity.OrderStatus}</td> <td>orders.status</td><td>枚举 @Enumerated(EnumType.STRING)</td><td>—</td></tr>
 * </table>
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li><b>cart_items 联合唯一约束</b> {@code (user_id, book_id)} —— 同一用户对同一本书只占一行；重复加车走「数量累加」分支</li>
 *   <li><b>order_items 字段快照</b>（{@code book_title}、{@code book_image}、{@code unit_price}）—— 即使书后续改价/改名，历史订单仍保留下单时刻的数据</li>
 *   <li><b>Order.items 用 EAGER + Cascade.ALL</b> —— 保存 Order 时自动级联保存 OrderItem；查 Order 一并把 items 加载好</li>
 *   <li><b>@PrePersist 自动填写 createdAt</b> —— 在 INSERT 前由 JPA 回调注入当前时间</li>
 * </ul>
 */
package com.homework.bookstore.entity;
