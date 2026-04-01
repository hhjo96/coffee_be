package com.example.coffee_be.domain.pointHistory.service;

import com.example.coffee_be.common.entity.PointHistory;
import com.example.coffee_be.common.model.kafka.event.OrderCompletedEvent;
import com.example.coffee_be.domain.point.enums.PointStatus;
import com.example.coffee_be.domain.pointHistory.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointHistoryService {

    private final PointHistoryRepository historyRepository;


    public void savePointHistory(OrderCompletedEvent event) {
        PointHistory history = PointHistory.createPointHistory(event.getUserId(), event.getTotalPrice(), PointStatus.USED,
                LocalDateTime.parse(event.getPaidAt(), ISO_LOCAL_DATE_TIME));
        historyRepository.save(history);
    }

}
