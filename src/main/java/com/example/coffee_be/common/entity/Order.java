package com.example.coffee_be.common.entity;

import com.example.coffee_be.domain.order.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "orders"
        //,
        //   indexes = {
        //           @Index(name = "idx_auction_start", columnList = "start_at DESC, id DESC")
        //   }
)

public class Order extends BaseEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Long cartId;

    @Enumerated(value = EnumType.STRING)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private int price;

    public static Order createOrder(Long customerId, Long cartId, int price) {
        Order order = new Order();
        order.customerId = customerId;
        order.cartId = cartId;
        order.orderStatus = OrderStatus.PREPARING;
        order.price = price;

        return order;
    }

    public void prepareToReady() {
        if(this.orderStatus == OrderStatus.PREPARING) {
            this.orderStatus = OrderStatus.READY;
        }

    }

}
