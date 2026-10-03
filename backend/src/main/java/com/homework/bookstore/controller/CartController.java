package com.homework.bookstore.controller;

import com.homework.bookstore.dto.AddToCartRequest;
import com.homework.bookstore.dto.ApiResponse;
import com.homework.bookstore.dto.CartItemDto;
import com.homework.bookstore.dto.UpdateCartItemRequest;
import com.homework.bookstore.service.CartService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")  // 本类下所有端点前缀 /api/v1/cart
public class CartController {

    private final CartService cartService;  // 只依赖业务接口

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // GET /api/v1/cart?userId=1   查购物车列表
    @GetMapping
    public ApiResponse<List<CartItemDto>> list(@RequestParam Long userId) {
        return ApiResponse.success(cartService.listCart(userId));
    }

    // POST /api/v1/cart/items     加入购物车
    // @Valid 触发 AddToCartRequest 上的 @NotNull/@Min 等校验，失败由 GlobalExceptionHandler 接管
    @PostMapping("/items")
    public ApiResponse<CartItemDto> addItem(@Valid @RequestBody AddToCartRequest request) {
        CartItemDto dto = cartService.addToCart(
                request.getUserId(),
                request.getBookId(),
                request.getQuantity()
        );
        return ApiResponse.success("已加入购物车", dto);
    }

    // PUT /api/v1/cart/items/{id}   修改某条购物车的数量
    @PutMapping("/items/{id}")
    public ApiResponse<CartItemDto> updateItem(@PathVariable Long id,
                                               @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.success(cartService.updateQuantity(id, request.getQuantity()));
    }

    // DELETE /api/v1/cart/items/{id}   删除单条购物车明细
    @DeleteMapping("/items/{id}")
    public ApiResponse<Void> removeItem(@PathVariable Long id) {
        cartService.removeItem(id);
        return ApiResponse.success("已删除", null);  // 没数据返就传 null
    }

    // DELETE /api/v1/cart?userId=1   清空整车（下单后端内部也会调）
    @DeleteMapping
    public ApiResponse<Void> clear(@RequestParam Long userId) {
        cartService.clearCart(userId);
        return ApiResponse.success("已清空", null);
    }
}
