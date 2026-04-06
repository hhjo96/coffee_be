package com.example.coffee_be.common.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "menus"
        //,
        //   indexes = {
        //           @Index(name = "idx_auction_start", columnList = "start_at DESC, id DESC")
        //   }
)

public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30, nullable = false)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(length = 30)
    private String description;

    private LocalDateTime deletedAt;

    public static Menu createMenu(String name, int price, String description) {
        Menu menu = new Menu();
        menu.name = name;
        menu.price = price;
        menu.description = description;

        return menu;
    }

    public void changePrice(int price) {
        this.price = price;
    }

    public void deleteMenu() {
        this.deletedAt = LocalDateTime.now();
    }
}

