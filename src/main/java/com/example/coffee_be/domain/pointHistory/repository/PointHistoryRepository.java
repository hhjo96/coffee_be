package com.example.coffee_be.domain.pointHistory.repository;

import com.example.coffee_be.common.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    List<PointHistory> findByCustomerId(Long customerId);
}
