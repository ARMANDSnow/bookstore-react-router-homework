package com.homework.bookstore.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.homework.bookstore.entity.Book;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * {@link BookRepository} 的数据层切片测试 —— 验证"方法名/JPQL 真的被翻译成了正确的 SQL"。
 *
 * <h3>知识点 1：@DataJpaTest 是"切片测试"（test slice）</h3>
 * 不启动完整应用，只装配 JPA 相关的 Bean（EntityManager、DataSource、各 Repository），
 * Controller/Service/Security 统统不加载 —— 比 @SpringBootTest 快得多。
 * 它与 Mockito 单测的分工：单测验证"业务逻辑对不对"，本测试验证
 * "Spring Data 生成的 SQL 对不对"——后者 mock 不出来，必须真跑数据库。
 *
 * <h3>知识点 2：数据库从哪来？</h3>
 * {@code @AutoConfigureTestDatabase(replace = NONE)} 表示"不要替换我配置的数据源"，
 * 于是用的是 src/test/resources/application.yml 里的 <b>H2 内存库（MODE=MySQL 兼容模式）</b>：
 * 测试进程内建表（ddl-auto: create-drop）→ 插数 → 断言 → 进程结束库即销毁，
 * 不需要本机装 MySQL，任何机器 clone 下来 mvn test 都能跑。
 *
 * <h3>知识点 3：每个测试方法自带事务且结束后回滚</h3>
 * @DataJpaTest 给每个 @Test 包一个事务，方法结束自动 rollback ——
 * 所以 @BeforeEach 插入的数据不会泄漏到下一个测试，测试之间天然隔离。
 *
 * <p>配合 test application.yml 里的 {@code org.hibernate.SQL: debug}，
 * 跑 {@code mvn test} 就能在控制台看到派生方法生成的真实 SQL（答辩演示点）。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;  // 注入的是 Spring Data 动态代理生成的实现

    /** Book 实体大多数列是 NOT NULL，工具方法统一填齐，测试用例只关心 id/title/author */
    private Book newBook(String id, String title, String author) {
        Book b = new Book();
        b.setId(id);
        b.setTitle(title);
        b.setAuthor(author);
        b.setPrice(new BigDecimal("50.00"));
        b.setOriginalPrice(new BigDecimal("60.00"));
        b.setCategory("tech");
        b.setCategoryName("计算机与技术");
        b.setCategoryLabel("计算机与技术 / 测试");
        b.setImage("/images/" + id + ".jpg");
        b.setRating("4.5 / 5.0");
        b.setDescription("测试描述");
        b.setSummary("测试摘要");
        b.setHighlight("测试亮点");
        b.setAudience("测试读者");
        b.setReview("测试评价");
        return b;
    }

    @BeforeEach
    void seed() {
        // 测试数据由测试自己造（不依赖 data.sql）：3 本书覆盖中文标题/英文作者两种匹配场景
        bookRepository.save(newBook("clean-code", "代码整洁之道", "Robert C. Martin"));
        bookRepository.save(newBook("design", "设计心理学", "Donald A. Norman"));
        bookRepository.save(newBook("three-body", "三体", "刘慈欣"));
    }

    @Test
    @DisplayName("派生查询-标题命中：'整洁' 只命中《代码整洁之道》")
    void derivedQueryMatchesTitle() {
        // 方法名被解析为: where lower(title) like '%整洁%' or lower(author) like '%整洁%'
        List<Book> hits = bookRepository
                .findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase("整洁", "整洁");

        assertEquals(1, hits.size());
        assertEquals("clean-code", hits.get(0).getId());
    }

    @Test
    @DisplayName("派生查询-作者命中且忽略大小写：'NORMAN' 命中 Donald A. Norman")
    void derivedQueryMatchesAuthorIgnoringCase() {
        // IgnoreCase 的实现：SQL 两侧都套 lower() —— 'NORMAN' 与 'Norman' 都被降为 'norman'
        List<Book> hits = bookRepository
                .findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase("NORMAN", "NORMAN");

        assertEquals(1, hits.size());
        assertEquals("设计心理学", hits.get(0).getTitle());
    }

    @Test
    @DisplayName("等价性：@Query JPQL 写法与派生方法结果完全一致（两种写法互为对照）")
    void jpqlAndDerivedQueryAreEquivalent() {
        // 同一关键字分别走两条路：方法名解析 vs 手写 JPQL
        Set<String> byDerived = bookRepository
                .findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase("体", "体")
                .stream().map(Book::getId).collect(Collectors.toSet());
        Set<String> byJpql = bookRepository.searchByKeyword("体")
                .stream().map(Book::getId).collect(Collectors.toSet());

        // '体' 命中《三体》；断言两种写法命中的集合一模一样
        assertTrue(byDerived.contains("three-body"));
        assertEquals(byDerived, byJpql,
                "派生方法与 JPQL 是同一查询的两种写法，结果必须一致");
    }
}
