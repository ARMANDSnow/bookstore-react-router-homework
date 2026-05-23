package com.homework.bookstore.service;

import com.homework.bookstore.dto.CartItemDto;
import java.util.List;

public interface CartService {

    List<CartItemDto> listCart(Long userId);

    CartItemDto addToCart(Long userId, String bookId, int quantity);

    CartItemDto updateQuantity(Long itemId, int quantity);

    void removeItem(Long itemId);

    void clearCart(Long userId);
}
