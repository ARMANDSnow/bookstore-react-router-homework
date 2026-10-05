package com.homework.bookstore.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/** 新增/修改书籍的请求体。 */
public class BookRequest {

    @Size(max = 80, message = "书籍 ID 不能超过 80 个字符")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$", message = "书籍 ID 需以字母或数字开头，仅含字母、数字、点、横线和下划线")
    private String id;

    @NotBlank(message = "书名不能为空")
    @Size(max = 120, message = "书名不能超过 120 个字符")
    private String title;

    @NotBlank(message = "作者不能为空")
    @Size(max = 120, message = "作者不能超过 120 个字符")
    private String author;

    @Size(max = 40, message = "ISBN 不能超过 40 个字符")
    private String isbn;

    @Size(max = 120, message = "出版社不能超过 120 个字符")
    private String publisher;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    @JsonDeserialize(using = StrictIntegerDeserializer.class)
    private Integer stock;

    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.00", message = "售价不能为负数")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "定价不能为负数")
    private BigDecimal originalPrice;

    @Size(max = 60, message = "分类编码不能超过 60 个字符")
    private String category;

    @Size(max = 80, message = "分类名称不能超过 80 个字符")
    private String categoryName;

    @Size(max = 120, message = "分类标签不能超过 120 个字符")
    private String categoryLabel;

    @NotBlank(message = "封面不能为空")
    @Size(max = 255, message = "封面地址不能超过 255 个字符")
    private String image;

    @Size(max = 40, message = "评分不能超过 40 个字符")
    private String rating;

    @Size(max = 80, message = "角标不能超过 80 个字符")
    private String badge;

    @Size(max = 255, message = "简介不能超过 255 个字符")
    private String description;

    private String summary;
    private String highlight;
    private String audience;
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
