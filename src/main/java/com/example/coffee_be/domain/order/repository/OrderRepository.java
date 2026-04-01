package com.example.coffee_be.domain.order.repository;

import com.example.coffee_be.common.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 삭제된 메뉴더라도 출력함(주문이 삭제된 경우는 미포함)
    @Query(value = """
            SELECT ci.menu_id, SUM(ci.quantity) AS order_count
            FROM orders o
            JOIN cart_items ci ON ci.cart_id = o.cart_id
            WHERE o.order_status != 'CANCELLED'
              AND o.created_at >= :since
            GROUP BY ci.menu_id
            ORDER BY order_count DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findPopularMenuIdsSince(
            @Param("since") LocalDateTime since,
            @Param("limit") int limit);
}
