package com.homework.bookstore.service;

import com.homework.bookstore.dto.OrderDto;
import java.util.List;
import java.util.Optional;

public interface OrderService {

    OrderDto placeOrder(Long userId);

    List<OrderDto> listOrders(Long userId);

    Optional<OrderDto> getOrder(Long orderId);
}
