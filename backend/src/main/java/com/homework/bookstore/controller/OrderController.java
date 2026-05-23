package com.homework.bookstore.controller;

import com.homework.bookstore.dto.ApiResponse;
import com.homework.bookstore.dto.OrderDto;
import com.homework.bookstore.service.OrderService;
import com.homework.bookstore.service.exception.BusinessException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<OrderDto> placeOrder(@RequestParam Long userId) {
        return ApiResponse.success("下单成功", orderService.placeOrder(userId));
    }

    @GetMapping
    public ApiResponse<List<OrderDto>> listOrders(@RequestParam Long userId) {
        return ApiResponse.success(orderService.listOrders(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDto> getOrder(@PathVariable Long id) {
        OrderDto dto = orderService.getOrder(id)
                .orElseThrow(() -> new BusinessException(40405, "订单不存在"));
        return ApiResponse.success(dto);
    }
}
