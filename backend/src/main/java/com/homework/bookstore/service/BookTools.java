package com.homework.bookstore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.homework.bookstore.entity.Book;
import com.homework.bookstore.repository.BookRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BookTools {
    private final BookRepository books;
    private final ObjectMapper mapper;
    public BookTools(BookRepository books, ObjectMapper mapper) { this.books = books; this.mapper = mapper; }

    public ObjectNode check_inventory(String isbn) {
        var matches = find(isbn);
        if (matches.isEmpty()) return error("BOOK_NOT_FOUND", "未找到该ISBN，请核对号码或查看书架中的图书");
        if (matches.size() > 1) return error("AMBIGUOUS_ISBN", "同一ISBN对应多个记录，请联系书店核对版本");
        Book book = matches.get(0);
        var result = identity(book).put("source", "本店数据库").put("checkedAt", Instant.now().toString());
        if (book.getStock() == null) return result.putNull("stock").put("availability", "库存待确认");
        return result.put("stock", book.getStock()).put("availability", book.getStock() > 0 ? "有货" : "缺货");
    }
    /** Deterministic coursework fixture, never advertised as a scraped live price. */
    public ObjectNode get_competitor_price(String isbn) {
        var matches = find(isbn);
        if (matches.isEmpty()) return error("BOOK_NOT_FOUND", "模拟报价库没有该ISBN，请核对图书");
        if (matches.size() > 1) return error("AMBIGUOUS_ISBN", "同一ISBN对应多个记录，请联系书店核对版本");
        Book book = matches.get(0);
        return identity(book).put("price", book.getPrice().multiply(new BigDecimal("0.92")).setScale(2, RoundingMode.HALF_UP))
                .put("currency", "CNY").put("store", "课程模拟竞价商店").put("simulated", true)
                .put("source", "课程模拟数据，非外部实时报价");
    }
    private List<Book> find(String isbn) {
        if (isbn == null) return List.of();
        String normalized = isbn.replaceAll("[\\s-]", "");
        if (!normalized.matches("\\d{13}")) return List.of();
        return books.findByNormalizedIsbn(normalized);
    }
    private ObjectNode identity(Book book) {
        return mapper.createObjectNode().put("ok", true).put("id", book.getId()).put("isbn", book.getIsbn())
                .put("title", book.getTitle()).put("ourPrice", book.getPrice());
    }
    public ObjectNode error(String code, String message) {
        return mapper.createObjectNode().put("ok", false).put("error", code).put("message", message);
    }
}
