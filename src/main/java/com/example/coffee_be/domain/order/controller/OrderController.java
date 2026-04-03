package com.example.coffee_be.domain.order.controller;

import com.example.coffee_be.common.response.BaseResponse;
import com.example.coffee_be.domain.order.model.dto.OrderDto;
import com.example.coffee_be.domain.order.model.dto.PopularMenuDto;
import com.example.coffee_be.domain.order.model.request.OrderRequest;
import com.example.coffee_be.domain.order.service.OrderLuaService;
import com.example.coffee_be.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderLuaService orderLuaService;

    // 주문+ 포인트 차감
    @PostMapping
    public ResponseEntity<BaseResponse<OrderDto>> order(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success("200", "주문 성공", orderService.order(request)));
    }
    // 7일간 인기 메뉴 3개 조회
    @GetMapping("/popular")
    public ResponseEntity<BaseResponse<List<PopularMenuDto>>> getPopularMenus() {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success("200", "인기 메뉴 조회 성공", orderService.getPopularMenus()));
    }

    // 루아스크립트를 활용한 api(주문 + 포인트차감)
    @PostMapping("/lua")
    public ResponseEntity<BaseResponse<OrderDto>> orderWithLua(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success("200", "루아스크립트를 활용한 주문 성공",
                orderLuaService.orderLua(request)));
    }

}
