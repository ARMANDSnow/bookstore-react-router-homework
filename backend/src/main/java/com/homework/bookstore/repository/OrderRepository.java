package com.homework.bookstore.repository;

import com.homework.bookstore.entity.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 订单数据访问接口（{@code orders} 表）。
 *
 * <h3>本接口同时演示了评分标准 A.ii 提到的两种持久化方法</h3>
 * <ol>
 *   <li><b>派生方法</b>：{@link #findByUser_IdOrderByCreatedAtDesc}
 *       —— Spring Data JPA 按方法名生成 JPQL：
 *       {@code select o from Order o where o.user.id = ?1 order by o.createdAt desc}
 *       <br>方法名规则：{@code findBy + 字段（驼峰，关联用下划线分隔）+ OrderBy + 字段 + Asc/Desc}</li>
 *   <li><b>{@code @Query} + 原生 SQL</b>：{@link #sumTotalAmountByUserId}
 *       —— 走 {@code nativeQuery = true}，完全自己写 SQL；
 *       {@code COALESCE} 把 null 转 0，避免没订单时返回 null 造成调用方 NPE</li>
 * </ol>
 *
 * <h3>没有实现类？</h3>
 * 是的。Spring Data JPA 在启动时通过 <b>JDK 动态代理</b> 给这个接口生成一个代理对象注入 Spring 容器。
 * 调用 {@code findById(1L)} 时实际走到代理 → 解析方法签名 → 生成 SQL → 交给 Hibernate 执行。
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 派生方法：查某用户的所有订单，按创建时间倒序。
     * <p>方法名 {@code findByUser_Id...} 中下划线表示走关联实体：
     * {@code Order.user}（@ManyToOne）的 {@code id} 字段。
     */
    List<Order> findByUser_IdOrderByCreatedAtDesc(Long userId);

    @Query("select distinct o from Order o"
            + " left join o.items i"
            + " where (:userId is null or o.user.id = :userId)"
            + " and (:startAt is null or o.createdAt >= :startAt)"
            + " and (:endAt is null or o.createdAt <= :endAt)"
            + " and (:bookName is null or lower(i.bookTitle) like lower(concat('%', :bookName, '%')))"
            + " order by o.createdAt desc")
    List<Order> findWithFilters(@Param("userId") Long userId,
                                @Param("startAt") LocalDateTime startAt,
                                @Param("endAt") LocalDateTime endAt,
                                @Param("bookName") String bookName);

    /**
     * 演示评分标准 A.ii「定制持久化方法」——使用原生 SQL 统计某用户的累计消费金额。
     * <p>当前未在业务中调用，是为答辩演示而留下的样例代码。
     */
    @Query(
            value = "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE user_id = :userId",
            nativeQuery = true
    )
    BigDecimal sumTotalAmountByUserId(@Param("userId") Long userId);
}
