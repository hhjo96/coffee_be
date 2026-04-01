package com.example.coffee_be.domain.cartItem.repository;

import com.example.coffee_be.common.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndMenuId(Long id, Long id1);

    void deleteAllByCartId(Long id);

    List<CartItem> findAllByCartId(Long cartId);

}
