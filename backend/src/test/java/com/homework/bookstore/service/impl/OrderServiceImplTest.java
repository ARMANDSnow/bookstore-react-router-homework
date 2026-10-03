package com.homework.bookstore.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.homework.bookstore.dto.OrderDto;
import com.homework.bookstore.entity.Book;
import com.homework.bookstore.entity.CartItem;
import com.homework.bookstore.entity.Order;
import com.homework.bookstore.entity.OrderItem;
import com.homework.bookstore.entity.User;
import com.homework.bookstore.repository.CartItemRepository;
import com.homework.bookstore.repository.OrderRepository;
import com.homework.bookstore.repository.UserRepository;
import com.homework.bookstore.service.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link OrderServiceImpl#placeOrder} 的单元测试 —— 下单是全项目最核心的事务方法，
 * 这里验证它的三条业务不变量：
 * <ol>
 *   <li><b>总额正确</b>：Σ(单价 × 数量)，BigDecimal 精确计算</li>
 *   <li><b>价格快照</b>：OrderItem 冗余保存下单时刻的书名/单价，与 Book 实体解耦</li>
 *   <li><b>下单后清空购物车</b>：deleteByUserId 必须被调用</li>
 * </ol>
 *
 * <h3>知识点：单元测试测不到 @Transactional 本身</h3>
 * {@code @Transactional} 的回滚是 Spring AOP 代理在真实容器里干的活；本测试直接
 * new 被测对象（没有代理），所以这里验证的是"业务前置校验会抛 RuntimeException"——
 * 而 BusinessException 继承 RuntimeException，在真实运行时它正是触发回滚的信号。
 * 换句话说：单测锁住"会抛异常"这个因，事务回滚这个果由 Spring 框架保证。
 *
 * <h3>知识点：BigDecimal 断言要用 compareTo 而不是 equals</h3>
 * {@code new BigDecimal("223.0").equals(new BigDecimal("223.00"))} 是 false
 * （equals 连精度 scale 一起比），而 {@code compareTo} 只比数值大小 —— 金额断言的标准写法。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    // ---------- 造数小工具 ----------

    private User user(long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("buyer");
        return u;
    }

    private Book book(String id, String title, String price) {
        Book b = new Book();
        b.setId(id);
        b.setTitle(title);
        b.setAuthor("某作者");
        b.setImage("/images/" + id + ".jpg");
        b.setPrice(new BigDecimal(price));
        b.setOriginalPrice(new BigDecimal(price));
        return b;
    }

    private CartItem cartItem(User u, Book b, int quantity) {
        CartItem item = new CartItem();
        item.setUser(u);
        item.setBook(b);
        item.setQuantity(quantity);
        return item;
    }

    @Test
    @DisplayName("下单成功：总额=Σ(单价×数量)、明细带价格快照、购物车被清空")
    void placeOrderComputesTotalAndSnapshotsAndClearsCart() {
        // ---------- given ----------
        User u = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        // 购物车两件：79.00 × 2 + 65.00 × 1 = 223.00
        when(cartItemRepository.findByUser_IdOrderByCreatedAtAsc(1L)).thenReturn(List.of(
                cartItem(u, book("clean-code", "代码整洁之道", "79.00"), 2),
                cartItem(u, book("design", "设计心理学", "65.00"), 1)));
        // 摆拍 save：模拟数据库分配订单号后原样返回
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(1000L);
            return o;
        });

        // ---------- when ----------
        OrderDto dto = orderService.placeOrder(1L);

        // ---------- then ----------
        // 1) 总额精确等于 223.00（BigDecimal 用 compareTo，见类 Javadoc）
        assertEquals(0, dto.getTotalAmount().compareTo(new BigDecimal("223.00")),
                "总额应为 79×2 + 65×1 = 223.00，实际是 " + dto.getTotalAmount());

        // 2) 捕获保存进库的 Order，检查级联明细与价格快照
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertEquals(2, saved.getItems().size(), "两种书应生成两条订单明细（级联保存）");

        OrderItem first = saved.getItems().get(0);
        // 价格快照：明细上的书名/单价是从 Book "复印"来的独立字段——
        // 将来书改名/改价，orders 历史数据不受影响（这是订单表冗余设计的意义）
        assertEquals("代码整洁之道", first.getBookTitle());
        assertEquals(0, first.getUnitPrice().compareTo(new BigDecimal("79.00")));
        assertEquals(2, first.getQuantity());

        // 3) 下单成功必须清空购物车（一条批量 DELETE）
        verify(cartItemRepository).deleteByUserId(1L);
    }

    @Test
    @DisplayName("空购物车下单：抛 40001，且绝不写 orders 表")
    void placeOrderRejectsEmptyCart() {
        // given：用户存在但购物车为空
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(cartItemRepository.findByUser_IdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        // when + then
        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.placeOrder(1L));
        assertEquals(40001, ex.getCode());

        // 校验失败发生在任何写操作之前 —— 真实运行时即使已写了一半，
        // RuntimeException 也会让 @Transactional 把整个事务回滚
        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteByUserId(anyLong());
    }

    @Test
    @DisplayName("用户不存在：抛 40401")
    void placeOrderRejectsUnknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.placeOrder(99L));
        assertEquals(40401, ex.getCode());
        verify(orderRepository, never()).save(any());
    }
}
