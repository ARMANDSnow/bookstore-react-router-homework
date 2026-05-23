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

@Service
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public CartServiceImpl(CartItemRepository cartItemRepository,
                           UserRepository userRepository,
                           BookRepository bookRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemDto> listCart(Long userId) {
        ensureUserExists(userId);
        return cartItemRepository.findByUser_IdOrderByCreatedAtAsc(userId)
                .stream()
                .map(CartItemDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CartItemDto addToCart(Long userId, String bookId, int quantity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(40402, "书籍不存在"));

        Optional<CartItem> existing = cartItemRepository.findByUser_IdAndBook_Id(userId, bookId);
        CartItem item;
        if (existing.isPresent()) {
            item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            item = new CartItem();
            item.setUser(user);
            item.setBook(book);
            item.setQuantity(quantity);
        }
        return CartItemDto.from(cartItemRepository.save(item));
    }

    @Override
    @Transactional
    public CartItemDto updateQuantity(Long itemId, int quantity) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(40403, "购物车条目不存在"));
        item.setQuantity(quantity);
        return CartItemDto.from(cartItemRepository.save(item));
    }

    @Override
    @Transactional
    public void removeItem(Long itemId) {
        if (!cartItemRepository.existsById(itemId)) {
            throw new BusinessException(40403, "购物车条目不存在");
        }
        cartItemRepository.deleteById(itemId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        ensureUserExists(userId);
        cartItemRepository.deleteByUserId(userId);
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(40401, "用户不存在");
        }
    }
}
