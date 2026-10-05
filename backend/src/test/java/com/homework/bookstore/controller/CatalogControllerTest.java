package com.homework.bookstore.controller;

import com.homework.bookstore.dto.BookRequest;
import com.homework.bookstore.service.BookService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:catalog-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"
})
@AutoConfigureMockMvc
@Transactional
class CatalogControllerTest {
    @Autowired MockMvc mvc;
    @Autowired BookService books;

    @BeforeEach
    void seed() {
        add("a", "微服务入门", "tech", "50.00");
        add("b", "微服务设计", "tech", "30.00");
        add("c", "小说", "fiction", "30.00");
    }

    private void add(String id, String title, String category, String price) {
        BookRequest request = new BookRequest();
        request.setId(id); request.setTitle(title); request.setAuthor("测试作者");
        request.setCategory(category); request.setStock(10); request.setPrice(new BigDecimal(price));
        request.setImage("/covers/test.jpg"); books.createBook(request);
    }

    @Test void paginatesWithStableOrderAndMetadata() throws Exception {
        mvc.perform(get("/api/books").param("size", "2").param("page", "2"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].id").value("c"))
            .andExpect(jsonPath("$.data.totalItems").value(3)).andExpect(jsonPath("$.data.totalPages").value(2));
    }
    @Test void combinesCategoryKeywordAndSortBeforePagination() throws Exception {
        mvc.perform(get("/api/books").param("category", "tech").param("keyword", " 微服务 ")
            .param("sort", "price-low").param("size", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].id").value("b"))
            .andExpect(jsonPath("$.data.totalItems").value(2));
    }
    @Test void breaksPriceTiesById() throws Exception {
        mvc.perform(get("/api/books").param("sort", "price-low"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].id").value("b"))
            .andExpect(jsonPath("$.data.items[1].id").value("c"));
    }
    @Test void emptyCategoryReturnsEmptyPage() throws Exception {
        mvc.perform(get("/api/books").param("category", "missing"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty())
            .andExpect(jsonPath("$.data.totalItems").value(0));
    }
    @Test void outOfRangePageIsEmpty() throws Exception {
        mvc.perform(get("/api/books").param("page", "999"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
    }
    @Test void rejectsInvalidPageSizeAndSort() throws Exception {
        for (String[] pair : new String[][]{{"page", "0"}, {"size", "0"}, {"size", "101"}, {"sort", "unknown"}}) {
            mvc.perform(get("/api/books").param(pair[0], pair[1])).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
        }
    }
    @Test void rejectsNonNumericPageAsClientError() throws Exception {
        mvc.perform(get("/api/books").param("page", "abc")).andExpect(status().isBadRequest());
    }
    @Test void preservesLegacyArrayResponse() throws Exception {
        mvc.perform(get("/api/v1/books")).andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
    }
    @Test void treatsSqlWildcardsAsLiteralSearchText() throws Exception {
        add("d", "Java_百分之%", "tech", "20.00");
        for (String keyword : new String[]{"%", "_"}) {
            mvc.perform(get("/api/books").param("keyword", keyword)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value("d"));
        }
    }
    @Test void rejectsPaginationOffsetOverflow() throws Exception {
        mvc.perform(get("/api/books").param("page", "2147483647").param("size", "100"))
            .andExpect(status().isBadRequest());
    }
    @Test void returnsSingleBookAtRestResourcePath() throws Exception {
        mvc.perform(get("/api/books/a")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("微服务入门"))
            .andExpect(jsonPath("$.data.stock").value(10));
    }
    @Test void missingBookReturns404() throws Exception {
        mvc.perform(get("/api/books/missing")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(40404));
    }
}
