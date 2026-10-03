/**
 * <h2>Controller 层 —— HTTP 入口</h2>
 *
 * <p>职责：
 * <ol>
 *   <li>用 {@code @RestController} + {@code @RequestMapping} 暴露 REST 端点</li>
 *   <li>解析 HTTP 请求参数（{@code @RequestBody} / {@code @RequestParam} / {@code @PathVariable}）</li>
 *   <li>用 {@code @Valid} 触发 JSR-380 参数校验</li>
 *   <li>调用 Service 层完成业务</li>
 *   <li>将结果包成 {@link com.homework.bookstore.dto.ApiResponse} 返回</li>
 * </ol>
 *
 * <p><b>不做什么：</b>不写业务逻辑、不直接调 Repository、不做事务管理。
 *
 * <h3>本包成员</h3>
 * <ul>
 *   <li>{@link com.homework.bookstore.controller.BookController}     —— 书籍列表、详情</li>
 *   <li>{@link com.homework.bookstore.controller.UserController}     —— 注册、登录</li>
 *   <li>{@link com.homework.bookstore.controller.CartController}     —— 购物车 CRUD</li>
 *   <li>{@link com.homework.bookstore.controller.OrderController}    —— 下单、订单查询</li>
 *   <li>{@link com.homework.bookstore.controller.GlobalExceptionHandler} —— 全局异常翻译</li>
 * </ul>
 *
 * <h3>端点一览</h3>
 * <pre>
 *   GET    /api/v1/books            书籍列表
 *   GET    /api/v1/book/{id}        书籍详情
 *   POST   /api/v1/users/register   注册
 *   POST   /api/v1/users/login      登录
 *   GET    /api/v1/cart?userId=     购物车列表
 *   POST   /api/v1/cart/items       加入购物车
 *   PUT    /api/v1/cart/items/{id}  改数量
 *   DELETE /api/v1/cart/items/{id}  删一项
 *   DELETE /api/v1/cart?userId=     清空购物车
 *   POST   /api/v1/orders?userId=   下单
 *   GET    /api/v1/orders?userId=   订单列表
 *   GET    /api/v1/orders/{id}      订单详情
 * </pre>
 */
package com.homework.bookstore.controller;
