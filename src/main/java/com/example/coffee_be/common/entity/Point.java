package com.example.coffee_be.common.entity;

import com.example.coffee_be.common.exception.ErrorEnum;
import com.example.coffee_be.common.exception.ServiceErrorException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "points"
        //,
        //   indexes = {
        //           @Index(name = "idx_auction_start", columnList = "start_at DESC, id DESC")
        //   }
)

// 유저의 현재 포인트 상태를 표시하는 용도(이력은 pointHistory). 유저와 1:1
public class Point extends BaseEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private int balance;

    @Version
    private Long version;

    // 첫 생성
    public static Point createPoint(Long customerId) {
        Point point = new Point();
        point.customerId = customerId;
        point.balance = 0;

        return point;
    }

    // 충전
    public int charge(int amount) {
        // 충전하는 양이 0이하인 경우
        if (amount <= 0) {
            throw new ServiceErrorException(ErrorEnum.POINT_CHARGE_UNDER_ZERO);
        }
        int before = this.balance;
        this.balance += amount;
        return before;
    }

    // 사용
    public int use(int amount) {
        // 사용하는 양이 0 미만인 경우
        if (amount < 0) {
            throw new ServiceErrorException(ErrorEnum.POINT_CHARGE_UNDER_ZERO);
        }
        if (this.balance < amount) {
            throw new ServiceErrorException(ErrorEnum.POINT_INSUFFICIENT);
        }
        int before = this.balance;
        this.balance -= amount;
        return before;
    }
}
