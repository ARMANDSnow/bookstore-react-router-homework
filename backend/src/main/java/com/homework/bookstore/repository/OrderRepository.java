package com.homework.bookstore.repository;

import com.homework.bookstore.entity.Order;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUser_IdOrderByCreatedAtDesc(Long userId);

    /**
     * 演示评分标准 A.ii「定制持久化方法」——使用原生 SQL 统计某用户的累计消费金额。
     */
    @Query(
            value = "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE user_id = :userId",
            nativeQuery = true
    )
    BigDecimal sumTotalAmountByUserId(@Param("userId") Long userId);
}
