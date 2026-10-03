package com.homework.bookstore.dto;

import java.math.BigDecimal;
import java.util.List;

/** 顾客个人购买统计汇总。 */
public class CustomerStatsDto {

    private Long userId;
    private String username;
    private Integer totalBooks;
    private BigDecimal totalAmount;
    private List<CustomerBookStatDto> books;

    public CustomerStatsDto(Long userId,
                            String username,
                            Integer totalBooks,
                            BigDecimal totalAmount,
                            List<CustomerBookStatDto> books) {
        this.userId = userId;
        this.username = username;
        this.totalBooks = totalBooks;
        this.totalAmount = totalAmount;
        this.books = books;
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

    public Integer getTotalBooks() {
        return totalBooks;
    }

    public void setTotalBooks(Integer totalBooks) {
        this.totalBooks = totalBooks;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<CustomerBookStatDto> getBooks() {
        return books;
    }

    public void setBooks(List<CustomerBookStatDto> books) {
        this.books = books;
    }
}
