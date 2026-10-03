package com.homework.bookstore.dto;

import java.math.BigDecimal;

/** 指定时间范围内的图书销量排行项。 */
public class SalesRankDto {

    private String bookId;
    private String bookTitle;
    private Integer quantity;
    private BigDecimal totalAmount;

    public SalesRankDto(String bookId, String bookTitle, Integer quantity, BigDecimal totalAmount) {
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
