package com.homework.bookstore.dto;

import com.homework.bookstore.entity.CartItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 给前端用的购物车明细 DTO，包含书籍快照字段，避免前端再发一次查书请求。
 *
 * <p>它把 {@code CartItem + Book} 两个实体的信息压平成一个 JSON 对象：
 * 前端表格可以直接渲染 title/image/price/quantity/subtotal，不需要理解 JPA 关联关系。
 */
public class CartItemDto {

    private Long id;
    private String bookId;
    private String title;
    private String author;
    private String image;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer quantity;
    private BigDecimal subtotal;
    private LocalDateTime updatedAt;

    /** 实体转 DTO：从 CartItem 的 book 关联对象取展示字段，并计算小计。 */
    public static CartItemDto from(CartItem item) {
        CartItemDto dto = new CartItemDto();
        dto.setId(item.getId());
        dto.setBookId(item.getBook().getId());
        dto.setTitle(item.getBook().getTitle());
        dto.setAuthor(item.getBook().getAuthor());
        dto.setImage(item.getBook().getImage());
        dto.setPrice(item.getBook().getPrice());
        dto.setOriginalPrice(item.getBook().getOriginalPrice());
        dto.setStock(item.getBook().getStock());
        dto.setQuantity(item.getQuantity());
        dto.setSubtotal(item.getBook().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
