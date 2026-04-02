package com.example.coffee_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CoffeeBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoffeeBeApplication.class, args);
    }

}
