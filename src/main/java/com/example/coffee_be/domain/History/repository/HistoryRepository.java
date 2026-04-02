package com.example.coffee_be.domain.History.repository;

import com.example.coffee_be.common.entity.History;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoryRepository extends JpaRepository<History, Long> {
    List<History> findByCustomerId(Long customerId);
}
