package com.homework.bookstore.repository;

import com.homework.bookstore.entity.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 * 购物车数据访问接口（{@code cart_items} 表）。
 *
 * <h3>查询方法</h3>
 * <ul>
 *   <li>{@link #findByUser_IdOrderByCreatedAtAsc}    —— 按下单顺序拿用户的所有购物车明细</li>
 *   <li>{@link #findByUser_IdAndBook_Id}             —— 「加车」前先查重，决定是 UPDATE 数量 还是 INSERT 新行</li>
 *   <li>{@link #deleteByUserId}                       —— 下单成功后清空该用户购物车</li>
 * </ul>
 *
 * <h3>{@code @Modifying} 是什么？</h3>
 * Spring Data JPA 默认认为 {@code @Query} 是查询。
 * 写 DELETE/UPDATE 必须显式加 {@code @Modifying}，否则会报错。
 * 同时建议在方法或 Service 上加 {@code @Transactional}，因为修改类操作必须在事务里。
 *
 * <h3>这条 JPQL 为什么不用派生方法 {@code deleteByUser_Id} ？</h3>
 * 派生 {@code deleteBy...} 实际上会先 SELECT 再逐行 DELETE，会触发 N+1 查询。
 * 写成 {@code @Modifying @Query} 一句 JPQL 直接走 {@code delete from ... where ...}，
 * 一次往返完事，对应一条 {@code delete from cart_items where user_id=?} 的 SQL。
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUser_IdOrderByCreatedAtAsc(Long userId);

    Optional<CartItem> findByUser_IdAndBook_Id(Long userId, String bookId);

    /**
     * 批量删除某用户的所有购物车明细（下单事务里调用）。
     *
     * @return 受影响行数
     */
    @Modifying
    @Transactional
    @Query("delete from CartItem c where c.user.id = :userId")
    int deleteByUserId(Long userId);
}
