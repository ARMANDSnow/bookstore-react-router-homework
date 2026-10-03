package com.homework.bookstore.controller;

import com.homework.bookstore.dto.ApiResponse;
import com.homework.bookstore.dto.BookDto;
import com.homework.bookstore.dto.BookRequest;
import com.homework.bookstore.service.BookService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class BookController {

    // Controller 只依赖 Service，不直接碰 Repository / Entity：
    // 数据怎么取、来自哪个数据库，都封装在 BookService 之后，对外只暴露 BookDto。
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // GET /api/v1/books                 返回所有书籍（迭代二原有行为，保持不变）
    // GET /api/v1/books?keyword=三体     标题/作者模糊搜索（迭代三新增）
    //
    // 用"可选参数"而不是新开 /books/search 端点：对老调用方完全向后兼容——
    // required=false 时不带 keyword 的请求 keyword 为 null，走原来的全量分支。
    @GetMapping("/books")
    public ApiResponse<List<BookDto>> getBooks(
            @RequestParam(value = "keyword", required = false) String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return ApiResponse.success(bookService.listBooks());
        }
        // trim：把 "  三体 " 这类前后空格去掉再查，避免 LIKE '% 三体 %' 搜不到
        return ApiResponse.success(bookService.searchBooks(keyword.trim()));
    }

    // GET /api/v1/book/{id}    按 id 查一本书（不存在时 Service 抛 40404 → HTTP 404）
    @GetMapping("/book/{id}")
    public ApiResponse<BookDto> getBook(@PathVariable String id) {
        return ApiResponse.success(bookService.getBook(id));
    }

    // POST /api/v1/books       管理员新增书籍
    @PostMapping("/books")
    public ApiResponse<BookDto> createBook(@Valid @RequestBody BookRequest request) {
        return ApiResponse.success("书籍已新增", bookService.createBook(request));
    }

    // PUT /api/v1/book/{id}    管理员修改书籍
    @PutMapping("/book/{id}")
    public ApiResponse<BookDto> updateBook(@PathVariable String id,
                                           @Valid @RequestBody BookRequest request) {
        return ApiResponse.success("书籍已更新", bookService.updateBook(id, request));
    }

    // DELETE /api/v1/book/{id} 管理员删除旧书
    @DeleteMapping("/book/{id}")
    public ApiResponse<Void> deleteBook(@PathVariable String id) {
        bookService.deleteBook(id);
        return ApiResponse.success("书籍已删除", null);
    }
}
