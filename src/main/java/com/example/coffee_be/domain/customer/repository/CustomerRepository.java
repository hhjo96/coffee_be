package com.example.coffee_be.domain.customer.repository;

import com.example.coffee_be.common.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
