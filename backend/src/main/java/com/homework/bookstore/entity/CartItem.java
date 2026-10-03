package com.homework.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "cart_items",
        // ★ 联合唯一约束 (user_id, book_id)：
        // 数据库层面保证「同一个用户对同一本书」最多只有一行购物车记录
        // 即使应用层 addToCart 的"先查后改"逻辑因并发竞争失效，
        // 这层约束也会让重复 INSERT 直接报错，数据不会脏。
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_book",
                columnNames = {"user_id", "book_id"}  // 注意是数据库列名，不是 Java 字段名
        )
)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 多对一：多条购物车明细可以属于同一个用户
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 多对一：多条明细可以引用同一本书
    // EAGER 是为了 CartItemDto.from(item) 时能直接拿到 book.title 等字段
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private Integer quantity;       // 购物车里这本书的数量

    @Column(nullable = false)
    private LocalDateTime createdAt;  // 首次加入时间

    @Column(nullable = false)
    private LocalDateTime updatedAt;  // 最近一次修改时间（数量变化时刷新）

    // INSERT 前自动填两个时间
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    // UPDATE 前自动刷新 updatedAt
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
