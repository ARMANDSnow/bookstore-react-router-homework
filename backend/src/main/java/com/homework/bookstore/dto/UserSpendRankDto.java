package com.homework.bookstore.dto;

import java.math.BigDecimal;

/** 指定时间范围内的用户消费排行项。 */
public class UserSpendRankDto {

    private Long userId;
    private String username;
    private Integer orderCount;
    private BigDecimal totalAmount;

    public UserSpendRankDto(Long userId, String username, Integer orderCount, BigDecimal totalAmount) {
        this.userId = userId;
        this.username = username;
        this.orderCount = orderCount;
        this.totalAmount = totalAmount;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
