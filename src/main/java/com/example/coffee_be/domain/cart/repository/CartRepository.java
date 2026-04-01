package com.example.coffee_be.domain.cart.repository;

import com.example.coffee_be.common.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByCustomerId(Long userId);
}
