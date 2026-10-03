package com.homework.bookstore.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 修改购物车数量请求体。
 *
 * <p>itemId 放在 URL 路径 {@code /cart/items/{id}} 中，body 里只传可变字段 quantity。
 * 这是 REST 风格里常见的"路径定位资源，body 描述修改内容"。
 */
public class UpdateCartItemRequest {

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    private Integer quantity;

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
