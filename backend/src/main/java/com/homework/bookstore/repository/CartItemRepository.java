package com.homework.bookstore.repository;

import com.homework.bookstore.entity.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUser_IdOrderByCreatedAtAsc(Long userId);

    Optional<CartItem> findByUser_IdAndBook_Id(Long userId, String bookId);

    @Modifying
    @Transactional
    @Query("delete from CartItem c where c.user.id = :userId")
    int deleteByUserId(Long userId);
}
