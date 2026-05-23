package com.homework.bookstore.service.impl;

import com.homework.bookstore.dto.OrderDto;
import com.homework.bookstore.entity.CartItem;
import com.homework.bookstore.entity.Order;
import com.homework.bookstore.entity.OrderItem;
import com.homework.bookstore.entity.OrderStatus;
import com.homework.bookstore.entity.User;
import com.homework.bookstore.repository.CartItemRepository;
import com.homework.bookstore.repository.OrderRepository;
import com.homework.bookstore.repository.UserRepository;
import com.homework.bookstore.service.OrderService;
import com.homework.bookstore.service.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            CartItemRepository cartItemRepository,
                            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
    }

    /**
     * 下单：读购物车 -> 生成订单与明细 -> 清空购物车，整体放在一个事务里，
     * 任何一步抛错都会让前面对数据库的修改全部回滚。
     */
    @Override
    @Transactional
    public OrderDto placeOrder(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在"));

        List<CartItem> cartItems = cartItemRepository.findByUser_IdOrderByCreatedAtAsc(userId);
        if (cartItems.isEmpty()) {
            throw new BusinessException(40001, "购物车为空，无法下单");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PAID);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            OrderItem oi = new OrderItem();
            oi.setBook(cartItem.getBook());
            oi.setBookTitle(cartItem.getBook().getTitle());
            oi.setBookImage(cartItem.getBook().getImage());
            oi.setUnitPrice(cartItem.getBook().getPrice());
            oi.setQuantity(cartItem.getQuantity());
            order.addItem(oi);

            total = total.add(cartItem.getBook().getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);
        cartItemRepository.deleteByUserId(userId);
        return OrderDto.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> listOrders(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(40401, "用户不存在");
        }
        return orderRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(OrderDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderDto> getOrder(Long orderId) {
        return orderRepository.findById(orderId).map(OrderDto::from);
    }
}
