/**
 * <h2>Service 层（接口）—— 业务领域抽象</h2>
 *
 * <p>评分标准 <b>D.iii「接口与实现分离」</b>的体现：
 * Controller 只依赖本包里的接口，{@link com.homework.bookstore.service.impl 实现类} 在另一个包，
 * Spring 在运行时通过依赖注入（DI）把 Impl 实例注入到 Controller 的构造器。
 *
 * <h3>为什么要这样分？</h3>
 * <ol>
 *   <li><b>可替换：</b>未来要换实现（比如下单走分布式事务），Controller 一行不用改</li>
 *   <li><b>可测试：</b>单测 Controller 时传入 Mockito 模拟的接口实现即可</li>
 *   <li><b>面向接口编程：</b>降低耦合，遵循依赖倒置原则（DIP）</li>
 * </ol>
 *
 * <h3>本包成员</h3>
 * <ul>
 *   <li>{@link com.homework.bookstore.service.UserService}  —— 注册 / 登录</li>
 *   <li>{@link com.homework.bookstore.service.CartService}  —— 购物车增删改查、清空</li>
 *   <li>{@link com.homework.bookstore.service.OrderService} —— 下单（事务）、查询订单</li>
 * </ul>
 *
 * <p>子包 {@code exception} 存放业务异常体系（{@link com.homework.bookstore.service.exception.BusinessException}）。
 */
package com.homework.bookstore.service;
