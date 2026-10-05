package com.homework.bookstore.service;

import com.homework.bookstore.dto.BookDto;
import com.homework.bookstore.dto.BookRequest;
import com.homework.bookstore.dto.BookPageDto;
import java.util.List;

/**
 * 书籍业务接口。
 *
 * <p>把数据访问（Repository）封装在 Service 之后，Controller 只面向本接口编程，
 * 不直接依赖 {@code BookRepository}，也不感知底层用的是哪种数据库
 * （作业 5 要求：DTO 层 + Service 对 Repository 层进行封装）。
 */
public interface BookService {

    BookPageDto listCatalog(int page, int size, String category, String keyword, String sort);

    /** 查询全部书籍。 */
    List<BookDto> listBooks();

    /** 按 id 查询单本书籍，不存在则抛业务异常（→ HTTP 404）。 */
    BookDto getBook(String id);

    /**
     * 按关键字模糊搜索书籍（迭代三新增）。
     *
     * <p>匹配规则：标题 或 作者 包含关键字（忽略大小写），底层是 SQL 的
     * {@code LIKE '%关键字%'}。搜不到时返回空列表（不是异常——"没有结果"是正常业务状态）。
     *
     * @param keyword 搜索关键字（调用方保证非空；空关键字的分流在 Controller 层做）
     */
    List<BookDto> searchBooks(String keyword);

    /** 管理员新增书籍。 */
    BookDto createBook(BookRequest request);

    /** 管理员修改书籍属性。 */
    BookDto updateBook(String id, BookRequest request);

    /** 管理员删除旧书籍。 */
    void deleteBook(String id);
}
