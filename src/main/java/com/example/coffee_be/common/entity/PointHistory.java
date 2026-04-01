package com.example.coffee_be.common.entity;

import com.example.coffee_be.domain.point.enums.PointStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_histories"
        //,
        //   indexes = {
        //           @Index(name = "idx_auction_start", columnList = "start_at DESC, id DESC")
        //   }
)
@Getter
@NoArgsConstructor
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    private PointStatus status;

    private LocalDateTime payedAt;

    public static PointHistory createPointHistory(Long customerId, int amount, PointStatus status, LocalDateTime payedAt ) {
        PointHistory pointHistory = new PointHistory();
        pointHistory.customerId = customerId;
        pointHistory.amount = amount;
        pointHistory.status = status;
        pointHistory.payedAt = payedAt;

        return pointHistory;
    }
}
