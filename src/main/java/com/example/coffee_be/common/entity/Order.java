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

    // todo: cartId로 수정, 여러개 주문할 수 있도록 수정
    private Long menuId;

    @Enumerated(value = EnumType.STRING)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private int price;

    public static Order createOrder(Long customerId, Long menuId, int price) {
        Order order = new Order();
        order.customerId = customerId;
        order.menuId = menuId;
        order.orderStatus = OrderStatus.PREPARING;
        order.price = price;

        return order;
    }


}
