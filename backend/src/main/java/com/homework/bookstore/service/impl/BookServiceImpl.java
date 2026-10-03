package com.homework.bookstore.service.impl;

import com.homework.bookstore.dto.BookDto;
import com.homework.bookstore.dto.BookRequest;
import com.homework.bookstore.entity.Book;
import com.homework.bookstore.repository.BookRepository;
import com.homework.bookstore.service.BookService;
import com.homework.bookstore.service.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 书籍业务实现：基于 Spring Data JPA 读取 books 表，并把实体转为对外的 {@link BookDto}。
 *
 * <p>书籍是只读资源，因此仅有查询逻辑；写操作（save/delete）由 {@link BookRepository}
 * 继承自 {@code JpaRepository} 的方法提供，留待后续迭代使用。
 *
 * <h3>错误码约定</h3>
 * <ul>
 *   <li>{@code 40404} 书籍不存在（按 id 查询时）</li>
 * </ul>
 * 由 {@link com.homework.bookstore.controller.GlobalExceptionHandler} 统一翻译为 HTTP 状态码。
 */
@Service
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;  // 只依赖书籍表

    // 构造器注入
    public BookServiceImpl(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookDto> listBooks() {
        // SQL: select * from books —— JpaRepository.findAll()
        return bookRepository.findAll().stream()
                .map(BookDto::from)  // 实体 → DTO，屏蔽底层数据来源
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BookDto getBook(String id) {
        // SQL: select * from books where id = ? —— Optional 配 orElseThrow
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40404, "书籍不存在"));
        return BookDto.from(book);
    }

    @Override
    @Transactional(readOnly = true)  // 只读事务：告诉 Hibernate 不用做脏检查快照，纯查询更省
    public List<BookDto> searchBooks(String keyword) {
        // 调用派生查询（方法名 → SQL，见 BookRepository 注释）。
        // 注意：OR 连接的派生查询"每个条件各占一个参数"，所以同一个 keyword 要传两遍——
        // 第一个给 title LIKE，第二个给 author LIKE（这是派生查询的机制约束，答辩常问）。
        return bookRepository
                .findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(BookDto::from)  // 实体 → DTO：对外屏蔽实体与底层数据源
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookDto createBook(BookRequest request) {
        String id = normalize(request.getId());
        if (id == null) {
            throw new BusinessException(40003, "书籍 ID 不能为空");
        }
        if (bookRepository.existsById(id)) {
            throw new BusinessException(40903, "书籍 ID 已存在");
        }
        Book book = new Book();
        book.setId(id);
        applyRequest(book, request);
        return BookDto.from(bookRepository.save(book));
    }

    @Override
    @Transactional
    public BookDto updateBook(String id, BookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40404, "书籍不存在"));
        applyRequest(book, request);
        return BookDto.from(bookRepository.save(book));
    }

    @Override
    @Transactional
    public void deleteBook(String id) {
        if (!bookRepository.existsById(id)) {
            throw new BusinessException(40404, "书籍不存在");
        }
        bookRepository.deleteById(id);
    }

    private void applyRequest(Book book, BookRequest request) {
        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setIsbn(normalize(request.getIsbn()));
        book.setPublisher(normalize(request.getPublisher()));
        book.setStock(request.getStock());
        book.setPrice(request.getPrice());
        book.setOriginalPrice(request.getOriginalPrice() == null ? request.getPrice() : request.getOriginalPrice());
        book.setCategory(defaultText(request.getCategory(), "general"));
        book.setCategoryName(defaultText(request.getCategoryName(), "综合图书"));
        book.setCategoryLabel(defaultText(request.getCategoryLabel(), book.getCategoryName()));
        book.setImage(request.getImage().trim());
        book.setRating(defaultText(request.getRating(), "暂无评分"));
        book.setBadge(normalize(request.getBadge()));
        book.setDescription(defaultText(request.getDescription(), request.getTitle()));
        book.setSummary(defaultText(request.getSummary(), book.getDescription()));
        book.setHighlight(defaultText(request.getHighlight(), "暂无内容亮点。"));
        book.setAudience(defaultText(request.getAudience(), "适合对本书主题感兴趣的读者。"));
        book.setReview(defaultText(request.getReview(), "暂无读者评价。"));
        if (book.getOriginalPrice().compareTo(BigDecimal.ZERO) == 0) {
            book.setOriginalPrice(book.getPrice());
        }
    }

    private String defaultText(String value, String fallback) {
        String normalized = normalize(value);
        return normalized == null ? fallback : normalized;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
