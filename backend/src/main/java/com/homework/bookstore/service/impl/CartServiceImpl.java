package com.homework.bookstore.service.impl;

import com.homework.bookstore.dto.CartItemDto;
import com.homework.bookstore.entity.Book;
import com.homework.bookstore.entity.CartItem;
import com.homework.bookstore.entity.User;
import com.homework.bookstore.repository.BookRepository;
import com.homework.bookstore.repository.CartItemRepository;
import com.homework.bookstore.repository.UserRepository;
import com.homework.bookstore.service.CartService;
import com.homework.bookstore.service.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 购物车业务实现。
 *
 * <h3>「加车」的关键策略</h3>
 * {@link #addToCart} 走「<b>存在则数量累加，不存在则插入</b>」逻辑，避免同一用户对同一本书在 cart_items 里出现多行。
 * 这条策略由两层保证：
 * <ol>
 *   <li>应用层：先 {@code findByUser_IdAndBook_Id} 判断，命中就 quantity += n</li>
 *   <li>数据库层：{@code cart_items} 表对 {@code (user_id, book_id)} 加了 UNIQUE 约束（uk_user_book）。
 *       即便并发竞争导致两个事务都判断成「不存在」并 INSERT，第二条会被数据库拒绝抛错，保护数据完整性</li>
 * </ol>
 *
 * <h3>事务策略</h3>
 * <ul>
 *   <li>{@code listCart} 是只读查询：{@code @Transactional(readOnly = true)}，
 *       Hibernate 可以省去 dirty-check，性能更好</li>
 *   <li>其它涉及写的操作都是默认读写事务</li>
 * </ul>
 *
 * <h3>清空购物车</h3>
 * {@link #clearCart} 调 {@link com.homework.bookstore.repository.CartItemRepository#deleteByUserId}，
 * 后者用 {@code @Modifying @Query} 一句 JPQL 批量删除，比循环 delete 效率高得多。
 */
@Service  // 注册为 Spring Bean
public class CartServiceImpl implements CartService {

    // 三个依赖：购物车表、用户表（用于校验）、书籍表（用于校验）
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    // 构造器注入
    public CartServiceImpl(CartItemRepository cartItemRepository,
                           UserRepository userRepository,
                           BookRepository bookRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    @Override
    @Transactional(readOnly = true)  // 只读事务，性能更好
    public List<CartItemDto> listCart(Long userId) {
        ensureUserExists(userId);  // 校验用户存在，避免传错 id 静默返空
        // SQL: select * from cart_items where user_id = ? order by created_at asc
        return cartItemRepository.findByUser_IdOrderByCreatedAtAsc(userId)
                .stream()
                .map(CartItemDto::from)  // 实体 → DTO（DTO 含书籍快照字段）
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CartItemDto addToCart(Long userId, String bookId, int quantity) {
        // 【1】校验用户存在 —— SQL: select * from users where id = ?
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在"));

        // 【2】校验书籍存在 —— SQL: select * from books where id = ?
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(40402, "书籍不存在"));

        // 【3】★ 关键策略：先查这个用户的购物车里有没有这本书
        // SQL: select * from cart_items where user_id = ? and book_id = ?
        Optional<CartItem> existing = cartItemRepository.findByUser_IdAndBook_Id(userId, bookId);
        int targetQuantity = existing.map(item -> item.getQuantity() + quantity).orElse(quantity);
        ensureEnoughStock(book, targetQuantity);

        CartItem item;
        if (existing.isPresent()) {
            // 已存在：数量累加（避免同一本书在购物车里出现多行）
            // SQL: update cart_items set quantity = ?, updated_at = ? where id = ?
            item = existing.get();
            item.setQuantity(targetQuantity);
        } else {
            // 不存在：新建一条
            // SQL: insert into cart_items (user_id, book_id, quantity, ...) values (...)
            item = new CartItem();
            item.setUser(user);
            item.setBook(book);
            item.setQuantity(quantity);
        }

        // save 在 JPA 里是"insert or update"：实体 id 为 null → insert；有 id → update
        return CartItemDto.from(cartItemRepository.save(item));
    }

    @Override
    @Transactional
    public CartItemDto updateQuantity(Long itemId, int quantity) {
        // 购物车里改数量 —— 先按主键查出来再 set
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(40403, "购物车条目不存在"));
        ensureEnoughStock(item.getBook(), quantity);
        item.setQuantity(quantity);
        // SQL: update cart_items set quantity = ?, updated_at = ? where id = ?
        return CartItemDto.from(cartItemRepository.save(item));
    }

    @Override
    @Transactional
    public void removeItem(Long itemId) {
        // 先 exists 校验，避免删不存在的条目时静默成功
        if (!cartItemRepository.existsById(itemId)) {
            throw new BusinessException(40403, "购物车条目不存在");
        }
        // SQL: delete from cart_items where id = ?
        cartItemRepository.deleteById(itemId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        ensureUserExists(userId);
        // 用 @Modifying @Query 一句批量 SQL 清空，效率优于循环 delete
        // SQL: delete from cart_items where user_id = ?
        cartItemRepository.deleteByUserId(userId);
    }

    /** 内部小工具：校验用户存在，不存在则抛业务异常 */
    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(40401, "用户不存在");
        }
    }

    private void ensureEnoughStock(Book book, int quantity) {
        Integer stock = book.getStock();
        if (stock != null && stock < quantity) {
            throw new BusinessException(40004, "库存不足：" + book.getTitle());
        }
    }
}
