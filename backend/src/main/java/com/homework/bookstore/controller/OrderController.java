package com.homework.bookstore.controller;

import com.homework.bookstore.dto.ApiResponse;
import com.homework.bookstore.dto.CustomerStatsDto;
import com.homework.bookstore.dto.OrderDto;
import com.homework.bookstore.dto.SalesRankDto;
import com.homework.bookstore.dto.UserSpendRankDto;
import com.homework.bookstore.service.OrderService;
import com.homework.bookstore.service.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// @RestController = @Controller + @ResponseBody，所有方法返回值自动序列化为 JSON
@RestController
@RequestMapping("/api/v1/orders")  // 类上的统一前缀，本类下所有端点都以 /api/v1/orders 开头
public class OrderController {

    // 控制器只依赖 Service 接口（不依赖具体实现），构造器注入
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // POST /api/v1/orders?userId=1
    // 下单接口：把当前购物车结成一张订单
    @PostMapping
    public ApiResponse<OrderDto> placeOrder(@RequestParam Long userId) {
        // 调 Service 完成事务，把返回的 DTO 包成 ApiResponse 返回
        return ApiResponse.success("下单成功", orderService.placeOrder(userId));
    }

    // GET /api/v1/orders?userId=1&startDate=2026-01-01&endDate=2026-01-31&bookName=三体
    // 订单列表：userId 为空时返回系统全部订单；不为空时返回某用户订单。
    @GetMapping
    public ApiResponse<List<OrderDto>> listOrders(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String bookName) {
        return ApiResponse.success(orderService.listOrders(
                userId,
                parseStart(startDate),
                parseEnd(endDate),
                bookName
        ));
    }

    // GET /api/v1/orders/{id}
    // 订单详情：按订单主键查一条
    @GetMapping("/{id}")
    public ApiResponse<OrderDto> getOrder(@PathVariable Long id) {
        // Optional → 不存在抛业务异常（→ 全局异常处理器转 HTTP 404）
        OrderDto dto = orderService.getOrder(id)
                .orElseThrow(() -> new BusinessException(40405, "订单不存在"));
        return ApiResponse.success(dto);
    }

    // GET /api/v1/orders/sales-rank?startDate=...&endDate=...
    @GetMapping("/sales-rank")
    public ApiResponse<List<SalesRankDto>> salesRank(@RequestParam(required = false) String startDate,
                                                     @RequestParam(required = false) String endDate) {
        return ApiResponse.success(orderService.listSalesRank(parseStart(startDate), parseEnd(endDate)));
    }

    // GET /api/v1/orders/user-spending-rank?startDate=...&endDate=...
    @GetMapping("/user-spending-rank")
    public ApiResponse<List<UserSpendRankDto>> userSpendRank(@RequestParam(required = false) String startDate,
                                                             @RequestParam(required = false) String endDate) {
        return ApiResponse.success(orderService.listUserSpendRank(parseStart(startDate), parseEnd(endDate)));
    }

    // GET /api/v1/orders/customer-stats?userId=1&startDate=...&endDate=...
    @GetMapping("/customer-stats")
    public ApiResponse<CustomerStatsDto> customerStats(@RequestParam Long userId,
                                                       @RequestParam(required = false) String startDate,
                                                       @RequestParam(required = false) String endDate) {
        return ApiResponse.success(orderService.getCustomerStats(userId, parseStart(startDate), parseEnd(endDate)));
    }

    private LocalDateTime parseStart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 10) {
            return LocalDate.parse(trimmed).atStartOfDay();
        }
        return LocalDateTime.parse(trimmed);
    }

    private LocalDateTime parseEnd(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 10) {
            return LocalDate.parse(trimmed).atTime(23, 59, 59);
        }
        return LocalDateTime.parse(trimmed);
    }
}
