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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
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
    private String newBookJson(String id) {
        return "{\"id\":\"" + id + "\",\"title\":\"新书\",\"author\":\"作者\",\"stock\":5,\"price\":12.50,\"image\":\"/cover.jpg\"}";
    }
    @Test void createsBookWith201LocationAndPersistentDetail() throws Exception {
        mvc.perform(post("/api/books").contentType("application/json").content(newBookJson("new-book")))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/books/new-book"));
        mvc.perform(get("/api/books/new-book")).andExpect(status().isOk()).andExpect(jsonPath("$.data.title").value("新书"));
    }
    @Test void duplicateIdReturnsConflict() throws Exception {
        mvc.perform(post("/api/books").contentType("application/json").content(newBookJson("a")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(40903));
    }
    @Test void rejectsMissingFields() throws Exception {
        mvc.perform(post("/api/books").contentType("application/json").content("{\"id\":\"new-book\"}"))
            .andExpect(status().isBadRequest());
    }
    @Test void rejectsNegativePriceAndStock() throws Exception {
        for (String json : new String[]{newBookJson("new").replace("12.50", "-1"), newBookJson("new").replace("\"stock\":5", "\"stock\":-1")}) {
            mvc.perform(post("/api/books").contentType("application/json").content(json)).andExpect(status().isBadRequest());
        }
    }
    @Test void malformedJsonIs400() throws Exception {
        mvc.perform(post("/api/books").contentType("application/json").content("{bad-json}"))
            .andExpect(status().isBadRequest());
    }
    @Test void preventsUnaddressableResourceId() throws Exception {
        mvc.perform(post("/api/books").contentType("application/json").content(newBookJson("path/book")))
            .andExpect(status().isBadRequest());
    }
    @Test void inventoryUpdatePreservesOtherFieldsAndSupportsZero() throws Exception {
        mvc.perform(patch("/api/books/a/inventory").contentType("application/json").content("{\"stock\":0}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.stock").value(0))
            .andExpect(jsonPath("$.data.title").value("微服务入门")).andExpect(jsonPath("$.data.price").value(50));
        mvc.perform(get("/api/books/a")).andExpect(jsonPath("$.data.stock").value(0));
    }
    @Test void inventoryRejectsInvalidTypesAndFields() throws Exception {
        for (String json : new String[]{"{}", "{\"stock\":null}", "{\"stock\":-1}", "{\"stock\":1.5}",
                "{\"stock\":\"3\"}", "{\"stock\":2147483648}", "{\"stock\":3,\"title\":\"覆盖\"}"}) {
            mvc.perform(patch("/api/books/a/inventory").contentType("application/json").content(json))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/books/a")).andExpect(jsonPath("$.data.stock").value(10));
    }
    @Test void inventoryMissingBookIs404() throws Exception {
        mvc.perform(patch("/api/books/missing/inventory").contentType("application/json").content("{\"stock\":5}"))
            .andExpect(status().isNotFound());
    }
    @Test void allowsInventoryRequestsFromRealFrontendOrigin() throws Exception {
        mvc.perform(options("/api/books/a/inventory").header("Origin", "http://127.0.0.1:5173")
                .header("Access-Control-Request-Method", "PATCH").header("Access-Control-Request-Headers", "content-type"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"));
        mvc.perform(patch("/api/books/a/inventory").header("Origin", "http://127.0.0.1:5173")
                .contentType("application/json").content("{\"stock\":4}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.stock").value(4));
    }
}
