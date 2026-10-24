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

    // 【作业4 · 得分点4.1：面向图书资源的REST查询】
    // GET /api/secure/books/{id}用路径表达“哪本书”，GET只读；权限检查先于Controller执行。
    // 成功返回HTTP200和统一JSON包装，不存在的图书由服务层/异常处理映射为404。
    // 缺失或错误JWT在Security链中返回401，不应进入这里，也不应返回空书冒充成功。
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookDto>> get(@PathVariable String id) throws InterruptedException {
        // 【作业4 · 得分点4.2：在真实HTTP线程上证明虚拟线程生效】
        // 模拟I/O等待由启动配置控制，外部请求不能任意放大延迟；这里没有额外new一个
        // 虚拟线程再报告它的类型，而是在实际处理查询的请求线程上调用currentThread。
        // 虚拟线程执行sleep时可释放承载它的平台线程，便于大量等待型请求并行推进。
        if (delayMs > 0) Thread.sleep(delayMs);
        var thread = Thread.currentThread();
        var book = books.getBook(id);
        // 响应头提供可观察证据：isVirtual、线程名、Java版本；配置文件启用与运行证据相互印证。
        // verify_assignment4.py再用64并发验证每个请求的头，不将单次成功等同于性能提升倍率。
        return ResponseEntity.ok()
            .header("X-Thread-Virtual", Boolean.toString(thread.isVirtual()))
            .header("X-Thread-Name", thread.getName())
            .header("X-Java-Version", System.getProperty("java.version"))
            .body(ApiResponse.success(book));
    }
}
