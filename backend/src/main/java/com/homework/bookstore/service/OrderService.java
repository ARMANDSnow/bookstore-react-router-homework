package com.homework.bookstore.service;

import com.homework.bookstore.dto.CustomerStatsDto;
import com.homework.bookstore.dto.OrderDto;
import com.homework.bookstore.dto.SalesRankDto;
import com.homework.bookstore.dto.UserSpendRankDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单业务接口。
 *
 * <p>订单是本项目最适合讲事务的领域：下单时要同时写 orders、order_items，
 * 并删除 cart_items。实现类把这些 SQL 包在一个 {@code @Transactional} 中，
 * 保证"订单生成"和"购物车清空"要么一起成功，要么一起回滚。
 */
public interface OrderService {

    /** 将某用户当前购物车结算为一张订单；空购物车或用户不存在时抛业务异常。 */
    OrderDto placeOrder(Long userId);

    /** 查询某用户的历史订单，按创建时间倒序排列。 */
    List<OrderDto> listOrders(Long userId);

    /** 按用户、时间范围和书名筛选订单；userId 为 null 时查询全部订单。 */
    List<OrderDto> listOrders(Long userId, LocalDateTime startAt, LocalDateTime endAt, String bookName);

    /**
     * 按订单 id 查详情。
     *
     * <p>这里返回 Optional，让 Controller 决定"查不到"应该转成什么业务异常和 HTTP 状态码。
     */
    Optional<OrderDto> getOrder(Long orderId);

    /** 管理员统计：指定时间范围内各书销量榜。 */
    List<SalesRankDto> listSalesRank(LocalDateTime startAt, LocalDateTime endAt);

    /** 管理员统计：指定时间范围内用户消费榜。 */
    List<UserSpendRankDto> listUserSpendRank(LocalDateTime startAt, LocalDateTime endAt);

    /** 顾客统计：指定时间范围内自己的购书情况。 */
    CustomerStatsDto getCustomerStats(Long userId, LocalDateTime startAt, LocalDateTime endAt);
}
