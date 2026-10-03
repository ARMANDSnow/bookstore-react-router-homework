package com.homework.bookstore.dto;

import com.homework.bookstore.entity.Book;
import java.math.BigDecimal;

/**
 * 书籍数据传输对象（DTO）。
 *
 * <p>作用：把对外的「视图」与底层 {@link Book} 实体解耦——Controller 只认 BookDto，
 * 不再直接把 JPA 实体当 JSON 返回。这样底层数据无论来自 MySQL、其它异构数据库，
 * 还是缓存，对外协议都保持稳定（作业 5 要求：增加 DTO 层屏蔽底层数据的具体来源）。
 *
 * <p>本 DTO 镜像 Book 的全部字段，序列化出的 JSON 与原先直接返回实体时一字不差，
 * 因此前端无需任何改动。
 */
public class BookDto {

    private String id;
    private String title;
    private String author;
    private String isbn;
    private String publisher;
    private Integer stock;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String category;
    private String categoryName;
    private String categoryLabel;
    private String image;
    private String rating;
    private String badge;
    private String description;
    private String summary;
    private String highlight;
    private String audience;
    private String review;

    /** 实体 → DTO：逐字段拷贝。 */
    public static BookDto from(Book book) {
        BookDto dto = new BookDto();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setIsbn(book.getIsbn());
        dto.setPublisher(book.getPublisher());
        dto.setStock(book.getStock());
        dto.setPrice(book.getPrice());
        dto.setOriginalPrice(book.getOriginalPrice());
        dto.setCategory(book.getCategory());
        dto.setCategoryName(book.getCategoryName());
        dto.setCategoryLabel(book.getCategoryLabel());
        dto.setImage(book.getImage());
        dto.setRating(book.getRating());
        dto.setBadge(book.getBadge());
        dto.setDescription(book.getDescription());
        dto.setSummary(book.getSummary());
        dto.setHighlight(book.getHighlight());
        dto.setAudience(book.getAudience());
        dto.setReview(book.getReview());
        return dto;
    }

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
