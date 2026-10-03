package com.homework.bookstore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * 书籍实体：映射数据库 {@code books} 表。
 *
 * <h3>答辩重点：为什么 Book 的主键不是自增 Long？</h3>
 * 其它实体（User / Order / CartItem / OrderItem）都使用数据库自增主键，
 * 但书籍这里故意使用业务语义 ID，例如 {@code three-body}、{@code clean-code}。
 * 这样前端路由 {@code /books/three-body} 更可读，种子数据也稳定；代价是新增书籍时
 * 应用层必须保证 id 不重复，数据库不会替我们自动生成。
 *
 * <h3>字段设计</h3>
 * 价格用 {@link java.math.BigDecimal}，对应数据库 DECIMAL，避免 double/float 存钱时的精度误差。
 * 简短字段用 VARCHAR；summary/highlight/audience/review 较长，使用 {@code @Lob + TEXT}。
 */
@Entity
@Table(name = "books")
public class Book {

    @Id
    @Column(length = 80)
    // 手动指定的业务主键，不加 @GeneratedValue；data.sql 中直接写入固定 id。
    private String id;

    // 以下字段基本对应详情页/列表页展示所需信息；实体只描述"怎么入库"，不处理展示逻辑。
    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 120)
    private String author;

    @Column(length = 40)
    private String isbn;

    @Column(length = 120)
    private String publisher;

    @Column
    private Integer stock;

    // 金额字段必须用 BigDecimal + DECIMAL(10,2)，避免浮点数精度问题。
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(nullable = false, length = 80)
    private String categoryName;

    @Column(nullable = false, length = 120)
    private String categoryLabel;

    @Column(nullable = false, length = 255)
    private String image;

    @Column(nullable = false, length = 40)
    private String rating;

    @Column(length = 80)
    private String badge;

    @Column(nullable = false, length = 255)
    private String description;

    // @Lob 表示大字段；columnDefinition="TEXT" 明确告诉 Hibernate 使用 MySQL TEXT 类型。
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String highlight;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String audience;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String review;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryLabel() {
        return categoryLabel;
    }

    public void setCategoryLabel(String categoryLabel) {
        this.categoryLabel = categoryLabel;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getHighlight() {
        return highlight;
    }

    public void setHighlight(String highlight) {
        this.highlight = highlight;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }
}
