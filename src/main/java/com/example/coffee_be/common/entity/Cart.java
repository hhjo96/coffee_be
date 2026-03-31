package com.example.coffee_be.common.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "carts"
        //,
     //   indexes = {
     //           @Index(name = "idx_auction_start", columnList = "start_at DESC, id DESC")
     //   }
        )

public class Cart extends BaseEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    public static Cart createCart(Long customerId) {
        Cart cart = new Cart();
        cart.customerId = customerId;

        return cart;
    }
}
