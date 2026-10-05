package com.homework.bookstore.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryUpdateRequest(
        @NotNull(message = "库存不能为空") @Min(value = 0, message = "库存不能为负数")
        @JsonDeserialize(using = StrictIntegerDeserializer.class) Integer stock) {
    @JsonAnySetter
    public void rejectExtra(String field, Object value) {
        throw new IllegalArgumentException("库存更新仅接受 stock 字段");
    }
}
