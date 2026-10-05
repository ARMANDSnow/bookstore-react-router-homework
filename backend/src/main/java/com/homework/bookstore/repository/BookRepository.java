package com.homework.bookstore.repository;

import com.homework.bookstore.entity.Book;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 书籍数据访问接口（Spring Data JPA）。
 *
 * <p>只写接口、没有实现类，但注入后能直接调用 —— 因为 Spring Data 在启动时
 * 用 <b>JDK 动态代理</b>为本接口生成实现：解析方法名/注解 → 拼出 JPQL → 交给
 * Hibernate 翻译成 SQL 执行（详见 ARCHITECTURE.md 第六节）。
 *
 * <h3>迭代三新增：关键字搜索的三种写法对照（答辩考点）</h3>
 * 下面两个方法功能完全等价（单元测试 BookRepositoryTest 中验证了结果一致），
 * 加上注释里的原生 SQL 共三种写法，取舍如下：
 * <ol>
 *   <li><b>派生查询（方法名生成）</b>：零 SQL，方法名即文档；缺点是条件一多名字爆炸
 *       （本方法两个条件名字就已经很长了）</li>
 *   <li><b>@Query + JPQL</b>：面向"实体和属性"而非"表和列"的查询语言，复杂条件可读性好；
 *       不绑定具体数据库（Hibernate 按方言翻译），可移植</li>
 *   <li><b>@Query(nativeQuery = true) + 原生 SQL</b>：可用数据库特有能力
 *       （如 MySQL 全文索引 MATCH ... AGAINST），代价是绑死方言、换库要改</li>
 * </ol>
 */
public interface BookRepository extends JpaRepository<Book, String> {
    @Query("select b from Book b where replace(replace(b.isbn, '-', ''), ' ', '') = :isbn order by b.id")
    List<Book> findByNormalizedIsbn(@Param("isbn") String isbn);

    @Query("select b from Book b where (:category is null or b.category = :category)"
            + " and (:keyword is null or lower(b.title) like lower(concat('%', :keyword, '%')) escape '!'"
            + " or lower(b.author) like lower(concat('%', :keyword, '%')) escape '!')")
    Page<Book> findCatalog(@Param("category") String category, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 【写法一：派生查询】按"标题 或 作者"模糊搜索，忽略大小写。业务代码实际使用的方法。
     *
     * <p>Spring Data 解析方法名的规则（按关键字拆分）：
     * <pre>
     * findBy + Title + Containing + IgnoreCase + Or + Author + Containing + IgnoreCase
     *          └属性┘  └LIKE %?%┘   └两侧统一大小写┘ └OR┘ └属性┘
     * </pre>
     * 生成的 SQL（跑 mvn test 时在日志里真实可见，org.hibernate.SQL=debug）：
     * <pre>
     * select ... from books
     *  where upper(title) like upper(?) escape '\'   -- ? = %关键字%
     *     or upper(author) like upper(?) escape '\'
     * </pre>
     * IgnoreCase 的落地方式由 Hibernate 按数据库方言决定（两侧统一套 upper() 或 lower()，
     * 效果等价）——这正是 JPQL/派生查询"面向实体、方言无关"的体现。
     *
     * <p>注意：OR 连接的每个条件都要占一个方法参数，所以调用方要把
     * <b>同一个关键字传两遍</b>（见 BookServiceImpl.searchBooks）。
     *
     * <p>性能提示（答辩考点）：{@code LIKE '%kw%'} 前置通配符会让 B+ 树索引失效
     * （无法按最左前缀定位），走全表扫描 —— 本项目 6 本书无所谓；
     * 数据量大时应改用全文索引或 Elasticsearch。
     */
    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String titleKeyword, String authorKeyword);

    /**
     * 【写法二：@Query + JPQL】与写法一完全等价的显式写法（教学对照用，测试中验证等价性）。
     *
     * <p>JPQL 面向实体模型：{@code Book b} / {@code b.title} 指的是实体类和属性，
     * 不是表名和列名 —— 由 Hibernate 根据 @Table/@Column 映射翻译成 SQL。
     * {@code :kw} 是命名参数，由 {@code @Param("kw")} 绑定，天然防 SQL 注入
     * （值走 PreparedStatement 占位符，不做字符串拼接）。
     *
     * <p>【写法三对照：原生 SQL】如果要用数据库特有语法，可写
     * <pre>
     * @Query(value = "select * from books where lower(title) like lower(concat('%', :kw, '%'))"
     *              + " or lower(author) like lower(concat('%', :kw, '%'))",
     *        nativeQuery = true)
     * </pre>
     * value 里就是原封不动发给 MySQL 的 SQL（from 后面是表名 books 而不是实体 Book）。
     */
    @Query("select b from Book b"
            + " where lower(b.title) like lower(concat('%', :kw, '%'))"
            + " or lower(b.author) like lower(concat('%', :kw, '%'))")
    List<Book> searchByKeyword(@Param("kw") String keyword);
}
