package com.homework.bookstore.entity;

/**
 * 订单状态枚举。
 * 存到数据库时配合 @Enumerated(EnumType.STRING) → 表里存的是 "PENDING" / "PAID" 字符串，
 * 不存 0/1，这样将来加新值不会错位。
 */
public enum OrderStatus {
    PENDING,   // 待支付（本项目未走过这个状态）
    PAID       // 已支付（本项目下单即支付完成）
}
