/**
 * <h2>DTO 层 —— 接口的输入输出契约</h2>
 *
 * <p>Data Transfer Object：在 Controller 和外部世界之间传递的对象，
 * 与 {@link com.homework.bookstore.entity 实体类} 解耦。
 *
 * <h3>为什么不直接返回实体？</h3>
 * <ol>
 *   <li><b>安全：</b>{@link com.homework.bookstore.entity.User} 含 {@code password}，直接序列化会泄密。
 *       对外只返 {@link com.homework.bookstore.dto.UserResponse}（不含 password）</li>
 *   <li><b>性能：</b>DTO 只挑前端要的字段，避免一次序列化几十个字段</li>
 *   <li><b>Jackson 兼容：</b>Hibernate 懒加载的代理对象直接序列化容易爆 LazyInitializationException</li>
 *   <li><b>接口稳定：</b>实体字段名重命名时，对外 DTO 字段名可以不动，向后兼容</li>
 * </ol>
 *
 * <h3>本包成员</h3>
 * <ul>
 *   <li>{@link com.homework.bookstore.dto.ApiResponse}            —— <b>统一响应外壳</b>{@code {code, message, data}}</li>
 *   <li>{@link com.homework.bookstore.dto.LoginRequest}           —— 登录请求体（含 @NotBlank 校验）</li>
 *   <li>{@link com.homework.bookstore.dto.RegisterRequest}        —— 注册请求体（含邮箱格式等校验）</li>
 *   <li>{@link com.homework.bookstore.dto.AddToCartRequest}       —— 加车请求体</li>
 *   <li>{@link com.homework.bookstore.dto.UpdateCartItemRequest}  —— 改数量请求体</li>
 *   <li>{@link com.homework.bookstore.dto.UserResponse}           —— 用户响应（去掉 password）</li>
 *   <li>{@link com.homework.bookstore.dto.CartItemDto}            —— 购物车明细（含书籍快照字段）</li>
 *   <li>{@link com.homework.bookstore.dto.OrderDto}               —— 订单响应（含订单明细列表）</li>
 *   <li>{@link com.homework.bookstore.dto.OrderItemDto}           —— 订单明细响应</li>
 * </ul>
 *
 * <h3>命名约定</h3>
 * <ul>
 *   <li>请求体类以 {@code Request} 结尾</li>
 *   <li>响应体类以 {@code Response} 或 {@code Dto} 结尾</li>
 *   <li>每个 Response/Dto 都有静态 {@code from(Entity)} 工厂方法负责映射</li>
 * </ul>
 */
package com.homework.bookstore.dto;
