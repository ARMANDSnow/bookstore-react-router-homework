package com.homework.bookstore.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.homework.bookstore.dto.CartItemDto;
import com.homework.bookstore.entity.Book;
import com.homework.bookstore.entity.CartItem;
import com.homework.bookstore.entity.User;
import com.homework.bookstore.repository.BookRepository;
import com.homework.bookstore.repository.CartItemRepository;
import com.homework.bookstore.repository.UserRepository;
import com.homework.bookstore.service.exception.BusinessException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link CartServiceImpl} 的单元测试 —— 重点覆盖「加车」的核心业务规则：
 * <b>同一用户同一本书：已存在则数量累加，不存在则新插一行</b>。
 *
 * <h3>知识点：@InjectMocks 与 @Mock 的配合</h3>
 * {@code @InjectMocks} 让 Mockito 替我们 new 出被测对象，并把上面声明的三个
 * {@code @Mock} 按<b>构造器参数类型</b>自动配对传入 —— 本质上是 Mockito 在测试里
 * "模拟"了一次 Spring 的构造器依赖注入。对照 {@code UserServiceImplTest} 里
 * 手动 new 的写法：两者等价，手动 new 更直白，@InjectMocks 更省事。
 */
@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookRepository bookRepository;

    @InjectMocks  // = new CartServiceImpl(cartItemRepository, userRepository, bookRepository)
    private CartServiceImpl cartService;

    // ---------- 造数小工具：单元测试的数据自己造，不依赖数据库 ----------

    private User user(long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("tester");
        return u;
    }

    private Book book(String id, String price) {
        Book b = new Book();
        b.setId(id);
        b.setTitle("书-" + id);
        b.setAuthor("作者-" + id);
        b.setImage("/images/" + id + ".jpg");
        b.setPrice(new BigDecimal(price));
        b.setOriginalPrice(new BigDecimal(price));
        return b;
    }

    @Test
    @DisplayName("首次加购：购物车没有这本书 → 新建条目，数量=入参")
    void addToCartInsertsNewItem() {
        // given：用户和书都存在，购物车里还没有这本书
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(bookRepository.findById("clean-code")).thenReturn(Optional.of(book("clean-code", "79.00")));
        when(cartItemRepository.findByUser_IdAndBook_Id(1L, "clean-code")).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        CartItemDto dto = cartService.addToCart(1L, "clean-code", 2);

        // then：走的是"插入"分支
        assertEquals(2, dto.getQuantity());
        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        CartItem savedItem = captor.getValue();
        assertNull(savedItem.getId(), "新条目 id 应为 null（由数据库自增分配），才会触发 INSERT 而非 UPDATE");
        assertEquals("clean-code", savedItem.getBook().getId());
    }

    @Test
    @DisplayName("重复加购：购物车已有这本书 → 数量累加而不是插新行（核心业务规则）")
    void addToCartAccumulatesQuantity() {
        // given：购物车里已有 quantity=2 的同款条目
        User u = user(1L);
        Book b = book("clean-code", "79.00");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(bookRepository.findById("clean-code")).thenReturn(Optional.of(b));

        CartItem existing = new CartItem();
        existing.setId(50L);  // 有主键 → save 时走 UPDATE
        existing.setUser(u);
        existing.setBook(b);
        existing.setQuantity(2);
        when(cartItemRepository.findByUser_IdAndBook_Id(1L, "clean-code")).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // when：再加 3 本
        CartItemDto dto = cartService.addToCart(1L, "clean-code", 3);

        // then：2 + 3 = 5，且更新的是原有那一行（id 不变）
        assertEquals(5, dto.getQuantity());
        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertEquals(50L, captor.getValue().getId(), "必须复用已有条目（UPDATE），不能插新行");
        // 补充说明：除应用层这个判断外，cart_items 表还有 (user_id, book_id) 唯一约束兜底，
        // 并发下即使两个事务同时判断"不存在"，第二个 INSERT 也会被数据库拒绝
    }

    @Test
    @DisplayName("加购失败：用户不存在 → 40401，且不应有任何写库动作")
    void addToCartRejectsUnknownUser() {
        // given：第一步查用户就失败（后续 bookRepository 根本不会被调用，无需摆拍）
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // when + then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> cartService.addToCart(99L, "clean-code", 1));
        assertEquals(40401, ex.getCode());
        verify(cartItemRepository, never()).save(any());
    }
}
