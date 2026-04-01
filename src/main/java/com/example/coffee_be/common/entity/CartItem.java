package com.example.coffee_be.common.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "cartItems"
        //,
        //   indexes = {
        //           @Index(name = "idx_auction_start", columnList = "start_at DESC, id DESC")
        //   }
)

public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long menuId;

    @Column(nullable = false)
    private String menuName;

    private int quantity;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private Long cartId;


    public static CartItem createCartItem(Long coffeeId, String coffeeName, int quantity, int price, Long cartId) {
        CartItem cartItem = new CartItem();
        cartItem.menuId = coffeeId;
        cartItem.menuName = coffeeName;
        cartItem.quantity = quantity;
        cartItem.price = price;
        cartItem.cartId = cartId;

        return cartItem;
    }

    public void changeQuantity(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        this.quantity = quantity;
    }
    public int totalPrice() {
        return this.price * this.quantity;
    }
}
