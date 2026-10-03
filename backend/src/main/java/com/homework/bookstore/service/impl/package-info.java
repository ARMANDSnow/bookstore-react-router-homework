/**
 * <h2>Service 实现层 —— 业务逻辑落地</h2>
 *
 * <p>职责：
 * <ol>
 *   <li>实现 {@link com.homework.bookstore.service} 包里的业务接口</li>
 *   <li>用 {@code @Service} 注解让 Spring 把它当作 Bean 管理</li>
 *   <li>用 {@code @Transactional} 圈定事务边界（方法正常返回则提交，抛异常则回滚）</li>
 *   <li>组合多个 Repository 操作完成一笔业务</li>
 *   <li>抛 {@link com.homework.bookstore.service.exception.BusinessException} 表达业务错</li>
 * </ol>
 *
 * <h3>本包成员</h3>
 * <ul>
 *   <li>{@link com.homework.bookstore.service.impl.UserServiceImpl}  —— 注册（防重）、登录（密码比对）</li>
 *   <li>{@link com.homework.bookstore.service.impl.CartServiceImpl}  —— 加车走「存在则累加，不存在则插入」</li>
 *   <li>{@link com.homework.bookstore.service.impl.OrderServiceImpl} —— <b>下单事务（核心）</b>：
 *       查购物车 → INSERT orders → INSERT order_items → 清空购物车，全部在一个 {@code @Transactional} 中</li>
 * </ul>
 */
package com.homework.bookstore.service.impl;
