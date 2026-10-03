package com.homework.bookstore.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity                     // 声明本类为 JPA 实体，会映射到数据库表
@Table(name = "orders")     // 指定表名为 orders（不写默认是类名小写）
public class Order {

    @Id                                                       // 主键
    @GeneratedValue(strategy = GenerationType.IDENTITY)       // 自增策略：交给数据库的 AUTO_INCREMENT
    private Long id;

    // 多对一：多个订单可以属于同一个用户
    // FetchType.LAZY：用到 user 字段时才发 SQL 去查（懒加载，避免不必要的 join）
    // optional = false：等价于 NOT NULL，订单必须有主人
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)           // 数据库列名 user_id，做外键
    private User user;

    // 订单总额；precision=10, scale=2 → DECIMAL(10,2)，最大 99999999.99
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // 订单状态用枚举：PENDING / PAID
    // @Enumerated(STRING) → 存字符串 "PAID"（如果用 ORDINAL 会存 0/1，加新枚举值会错位）
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;  // 下单时间，由 @PrePersist 自动填

    // 一对多：一个订单含多条明细
    // mappedBy = "order" → 关系由 OrderItem.order 字段维护（避免出现多余的中间表）
    // cascade = ALL → 保存订单时级联保存所有明细，删订单时级联删明细
    // orphanRemoval = true → 从 items 集合移除某项时，对应明细行也会被 DELETE
    // FetchType.EAGER → 查订单时一次性把明细也查出来（小数据量场景方便序列化）
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    // @PrePersist：JPA 生命周期回调，在 INSERT 前自动执行
    // 这里给 createdAt 和 status 兜底默认值，避免业务代码忘了设置
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = OrderStatus.PAID;
        }
    }

    // 双向关联维护：加明细的同时设置反向指针 item.order = this
    // 否则保存时 OrderItem.order_id 会是 null，违反 NOT NULL 约束
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}
