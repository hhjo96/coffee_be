package com.example.coffee_be.domain.menu.repository;

import com.example.coffee_be.common.entity.Menu;
import com.example.coffee_be.domain.menu.model.dto.MenuDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MenuRepository extends JpaRepository<Menu, Long>, MenuCustomRepository {
   @Query("SELECT new com.example.coffee_be.domain.menu.model.dto.MenuDto(m.id, m.name, m.price)" +
           " from Menu m where m.deletedAt is null")
    Page<MenuDto> getAllV1(Pageable pageable);
}
