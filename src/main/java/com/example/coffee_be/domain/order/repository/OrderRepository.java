package com.example.coffee_be.domain.order.repository;

import com.example.coffee_be.common.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query(value = """
            SELECT o.menu_id, COUNT(o.id) AS order_count
            FROM orders o
            WHERE o.order_status != 'CANCELLED'
              AND o.created_at >= :since
            GROUP BY o.menu_id
            ORDER BY order_count DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findPopularMenuIdsSince(
            @Param("since") LocalDateTime since,
            @Param("limit") int limit);
}
