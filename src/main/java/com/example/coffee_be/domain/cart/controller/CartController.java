package com.example.coffee_be.domain.cart.controller;

import com.example.coffee_be.common.response.BaseResponse;
import com.example.coffee_be.domain.cart.Service.CartService;
import com.example.coffee_be.domain.cart.model.dto.CartDto;
import com.example.coffee_be.domain.cart.model.request.AddCartItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/carts")
public class CartController {

    private final CartService cartService;

    // 메뉴 담기
    @PostMapping("/{userId}/items")
    public ResponseEntity<BaseResponse<CartDto>> addItem(
            @PathVariable Long userId,
            @Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success("200", "메뉴 담기 성공", cartService.addItem(userId, request)));
    }

    // 장바구니 조회
    @GetMapping("/{userId}")
    public ResponseEntity<BaseResponse<CartDto>> getCart(@PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success("200", "장바구니 조회 성공", cartService.getCart(userId)));
    }

    // 장바구니 전체 비우기
    @DeleteMapping("/{userId}/items")
    public ResponseEntity<BaseResponse<Void>> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success("200", "장바구니 비우기 성공", null));
    }
}
