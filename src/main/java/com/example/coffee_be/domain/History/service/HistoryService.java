package com.example.coffee_be.domain.History.service;

import com.example.coffee_be.common.entity.History;
import com.example.coffee_be.common.model.kafka.event.OrderCompletedEvent;
import com.example.coffee_be.domain.point.enums.PointStatus;
import com.example.coffee_be.domain.History.repository.HistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryService {

    private final HistoryRepository historyRepository;


    public void savePointHistory(OrderCompletedEvent event) {
        History history = History.createUseHistory(event.getUserId(), event.getTotalPrice(), event.getOrderId());
        historyRepository.save(history);
    }

}
