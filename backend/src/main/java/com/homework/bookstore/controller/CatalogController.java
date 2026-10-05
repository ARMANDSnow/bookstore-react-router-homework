package com.homework.bookstore.controller;

import com.homework.bookstore.dto.ApiResponse;
import com.homework.bookstore.dto.BookPageDto;
import com.homework.bookstore.dto.BookDto;
import com.homework.bookstore.dto.BookRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import com.homework.bookstore.service.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 体系架构作业的 REST 资源入口；旧 /api/v1 接口仍供原业务使用。 */
@RestController
@RequestMapping("/api/books")
public class CatalogController {
    private final BookService bookService;

    public CatalogController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/{id}")
    public ApiResponse<BookDto> get(@PathVariable String id) {
        return ApiResponse.success(bookService.getBook(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookDto>> create(@Valid @RequestBody BookRequest request) {
        BookDto book = bookService.createBook(request);
        var location = UriComponentsBuilder.fromPath("/api/books/{id}").buildAndExpand(book.getId()).encode().toUri();
        return ResponseEntity.created(location).body(ApiResponse.success("书籍已新增", book));
    }

    @GetMapping
    public ApiResponse<BookPageDto> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "recommended") String sort) {
        return ApiResponse.success(bookService.listCatalog(page, size, category, keyword, sort));
    }
}
