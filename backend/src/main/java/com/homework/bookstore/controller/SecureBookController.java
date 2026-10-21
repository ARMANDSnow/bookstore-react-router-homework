package com.homework.bookstore.controller;

import com.homework.bookstore.dto.ApiResponse;
import com.homework.bookstore.dto.BookDto;
import com.homework.bookstore.service.BookService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 受保护的 REST 图书资源。线程信息来自真正执行查询的请求线程。 */
@RestController
@RequestMapping("/api/secure/books")
public class SecureBookController {
    private final BookService books;
    private final long delayMs;
    public SecureBookController(BookService books,
            @Value("${bookstore.catalog.simulated-delay-ms:0}") long delayMs) {
        if (delayMs < 0 || delayMs > 5000) {
            throw new IllegalArgumentException("模拟查询延迟必须为 0 到 5000 毫秒");
        }
        this.books = books;
        this.delayMs = delayMs;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookDto>> get(@PathVariable String id) throws InterruptedException {
        // 模拟 I/O 等待，由启动配置控制，外部请求不能任意放大延迟。
        if (delayMs > 0) Thread.sleep(delayMs);
        var thread = Thread.currentThread();
        var book = books.getBook(id);
        return ResponseEntity.ok()
            .header("X-Thread-Virtual", Boolean.toString(thread.isVirtual()))
            .header("X-Thread-Name", thread.getName())
            .header("X-Java-Version", System.getProperty("java.version"))
            .body(ApiResponse.success(book));
    }
}
