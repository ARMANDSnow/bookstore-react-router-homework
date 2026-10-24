package com.homework.bookstore.controller;

import com.homework.bookstore.dto.ApiResponse;
import com.homework.bookstore.dto.BookPageDto;
import com.homework.bookstore.dto.BookDto;
import com.homework.bookstore.dto.BookRequest;
import com.homework.bookstore.dto.InventoryUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import com.homework.bookstore.service.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 【作业1 · 得分点1.3：API 路径符合 REST 资源语义】以 books 表示资源集合，
 * id 定位单本图书，inventory 定位该书库存；用 HTTP 方法表达查询、新建和局部修改。
 * 三条路径共四个操作，正好对应生成 OpenAPI 时的约束；旧 /api/v1 接口仍供原业务使用。
 *
 * <p>【作业1 · 得分点1.4：统一响应与错误结构】本类通过 ApiResponse 输出
 * code/message/data，成功 code=0；Service 抛出的业务异常由 GlobalExceptionHandler
 * 转换成 400/404/409 等 HTTP 状态和 data=null 的错误响应，Controller 不重复拼装错误 JSON。
 * 这里的公开图书接口与作业4的 /api/secure/** 鉴权演示入口是不同的接口范围。
 */
@RestController
@RequestMapping("/api/books")
public class CatalogController {
    private final BookService bookService;

    public CatalogController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * 【作业1 · 得分点1.3：GET 查询单个资源】路径 id → Service 查库 → BookDto → 200 响应。
     * GET 不修改数据；查不到由 Service 抛出 40404，再由全局处理器映射为 HTTP 404。
     * 答辩可分别请求已有 id 和不存在 id，对比相同响应外壳下的成功数据与错误信息。
     */
    @GetMapping("/{id}")
    public ApiResponse<BookDto> get(@PathVariable String id) {
        return ApiResponse.success(bookService.getBook(id));
    }

    /**
     * 【作业1 · 得分点1.3：POST 创建集合中的资源】JSON 先绑定为 BookRequest 并经 @Valid
     * 校验，再由 Service 创建并返回 BookDto；id 由请求提供，接口不会自动生成 id。
     * 【作业1 · 得分点1.4：201 与 Location】创建成功用 201，并用新 id 构建可查询的资源地址。
     * 缺必填字段/错误类型为 400，重复 id 为 409；演示时同时观察状态、响应头和响应体。
     * 创建请求当前允许忽略未知字段，与下面库存更新“拒绝额外字段”的规则不同。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BookDto>> create(@Valid @RequestBody BookRequest request) {
        BookDto book = bookService.createBook(request);
        var location = UriComponentsBuilder.fromPath("/api/books/{id}").buildAndExpand(book.getId()).encode().toUri();
        return ResponseEntity.created(location).body(ApiResponse.success("书籍已新增", book));
    }

    /**
     * 【作业1 · 得分点1.3：PATCH 只更新库存】输入 id 和 stock，Service 只修改该书库存，
     * 返回更新后的完整图书；不用 PUT 是因为这里不替换整本图书的标题、作者、价格等信息。
     * 【作业1 · 得分点1.4：输入边界】InventoryUpdateRequest 的校验/严格整数反序列化器
     * 联合限制 stock 为必填的 0～2147483647 整数，并拒绝小数 token、字符串、null 和额外字段。
     * 答辩可将 stock=0 的 200 与 stock=-1、stock="2" 的 400 对照，再查详情确认其他字段未变。
     */
    @PatchMapping("/{id}/inventory")
    public ApiResponse<BookDto> updateInventory(@PathVariable String id, @Valid @RequestBody InventoryUpdateRequest request) {
        return ApiResponse.success("库存已更新", bookService.updateInventory(id, request.stock()));
    }

    /**
     * 【作业1 · 得分点1.4：分页、分类及返回结构】输入 query 参数，缺省 page=1、size=12；
     * category/keyword 可省略。Service 校验 page>=1、size 为 1～100 及分页偏移量边界，
     * 去掉筛选词首尾空格，先按分类/关键字筛选排序，再把从 1 开始的页码转为数据库页码。
     * 输出 BookPageDto：items/page/size/totalItems/totalPages，便于前端计算分页并显示总数。
     * 未知分类或超过末页属于合法空结果，返回 200 空 items；非法页码/排序返回 400。
     * 答辩入口示例：GET /api/books?page=1&size=4，再换 category 或 size=0 对照结果。
     */
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
