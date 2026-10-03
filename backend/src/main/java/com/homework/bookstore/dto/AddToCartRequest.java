package com.homework.bookstore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 加入购物车请求体。
 *
 * <p>DTO 上的 Bean Validation 注解会在 Controller 参数 {@code @Valid} 时自动触发：
 * 校验失败不会进入 Service，而是抛 {@code MethodArgumentNotValidException}，
 * 再由 {@code GlobalExceptionHandler} 统一转成 {@code ApiResponse}。
 */
public class AddToCartRequest {

    // 迭代三仍采用教学简化：前端把 localStorage 中的 user.id 显式传给后端。
    @NotNull(message = "userId 不能为空")
    private Long userId;

    // 书籍主键是业务字符串 id，例如 three-body。
    @NotBlank(message = "bookId 不能为空")
    private String bookId;

    // 数量必须 >= 1；前端 InputNumber 也限制了 min=1，前后端双重校验。
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    private Integer quantity;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
