package com.homework.bookstore.controller;

import com.homework.bookstore.dto.ApiResponse;
import com.homework.bookstore.entity.Book;
import com.homework.bookstore.repository.BookRepository;
import com.homework.bookstore.service.exception.BusinessException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class BookController {

    private final BookRepository bookRepository;

    public BookController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GetMapping("/books")
    public ApiResponse<List<Book>> getBooks() {
        return ApiResponse.success(bookRepository.findAll());
    }

    @GetMapping("/book/{id}")
    public ApiResponse<Book> getBook(@PathVariable String id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(40404, "书籍不存在"));
        return ApiResponse.success(book);
    }
}
