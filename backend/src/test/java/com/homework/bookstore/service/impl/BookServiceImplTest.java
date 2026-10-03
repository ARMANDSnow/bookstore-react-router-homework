package com.homework.bookstore.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.homework.bookstore.dto.BookDto;
import com.homework.bookstore.entity.Book;
import com.homework.bookstore.repository.BookRepository;
import com.homework.bookstore.service.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link BookServiceImpl} 的单元测试 —— 实体→DTO 映射、404 语义、搜索委派。
 *
 * <p>Service 层测试的关注点不是 SQL（那是 {@code BookRepositoryTest} 的事），
 * 而是<b>业务编排</b>：调了哪个 Repository 方法、参数怎么传、实体怎么转 DTO、
 * 查不到时抛什么异常。
 */
@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book book(String id, String title, String author) {
        Book b = new Book();
        b.setId(id);
        b.setTitle(title);
        b.setAuthor(author);
        b.setPrice(new BigDecimal("50.00"));
        b.setOriginalPrice(new BigDecimal("60.00"));
        return b;
    }

    @Test
    @DisplayName("listBooks：实体列表被逐一映射为 DTO（对外不暴露 JPA 实体）")
    void listBooksMapsEntitiesToDtos() {
        when(bookRepository.findAll()).thenReturn(List.of(
                book("clean-code", "代码整洁之道", "Robert C. Martin"),
                book("three-body", "三体", "刘慈欣")));

        List<BookDto> dtos = bookService.listBooks();

        assertEquals(2, dtos.size());
        assertEquals("代码整洁之道", dtos.get(0).getTitle());
        assertEquals("three-body", dtos.get(1).getId());
    }

    @Test
    @DisplayName("getBook：书不存在 → 抛 40404（由全局异常处理器翻译成 HTTP 404）")
    void getBookThrowsWhenMissing() {
        when(bookRepository.findById("ghost")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> bookService.getBook("ghost"));
        assertEquals(40404, ex.getCode());
    }

    @Test
    @DisplayName("searchBooks：同一关键字必须传给派生方法的两个参数（title 与 author 各一个）")
    void searchBooksDelegatesKeywordTwice() {
        // OR 派生查询的机制约束：每个 OR 条件占一个方法参数，
        // 所以 Service 要把同一个关键字传两遍——本测试锁死这个容易写错的细节
        when(bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase("三体", "三体"))
                .thenReturn(List.of(book("three-body", "三体", "刘慈欣")));

        List<BookDto> dtos = bookService.searchBooks("三体");

        assertEquals(1, dtos.size());
        assertEquals("三体", dtos.get(0).getTitle());
        // verify 交互：确认 Service 调的是派生方法且两个参数都是同一关键字
        verify(bookRepository).findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase("三体", "三体");
    }

    @Test
    @DisplayName("searchBooks：没有命中时返回空列表（而不是抛异常——空结果是正常业务状态）")
    void searchBooksReturnsEmptyListWhenNoMatch() {
        when(bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase("不存在", "不存在"))
                .thenReturn(List.of());

        assertTrue(bookService.searchBooks("不存在").isEmpty());
    }
}
