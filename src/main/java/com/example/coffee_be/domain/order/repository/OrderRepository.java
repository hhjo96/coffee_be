package com.example.coffee_be.domain.order.repository;

import com.example.coffee_be.common.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
