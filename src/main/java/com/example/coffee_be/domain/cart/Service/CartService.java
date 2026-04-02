package com.example.coffee_be.domain.cart.Service;

import com.example.coffee_be.common.entity.Cart;
import com.example.coffee_be.common.entity.CartItem;
import com.example.coffee_be.common.entity.Menu;
import com.example.coffee_be.common.exception.ErrorEnum;
import com.example.coffee_be.common.exception.ServiceErrorException;
import com.example.coffee_be.domain.cart.model.dto.CartDto;
import com.example.coffee_be.domain.cart.model.request.AddCartItemRequest;
import com.example.coffee_be.domain.cart.repository.CartRepository;
import com.example.coffee_be.domain.cartItem.model.CartItemDto;
import com.example.coffee_be.domain.cartItem.repository.CartItemRepository;
import com.example.coffee_be.domain.menu.repository.MenuRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MenuRepository menuRepository;

    // 장바구니 조회
    // 없으면 없다고 반환
    public CartDto getCart(Long userId) {
        return cartRepository.findByCustomerId(userId)
                .map(cart -> {
                    log.info("[장바구니] 조회 성공 - userId={}", userId);
                    return buildCartDto(cart);
                })
                .orElseGet(() -> {
                    log.info("[장바구니] 없음 - userId={}", userId);
                    return new CartDto(null, userId, List.of(), 0);
                    // ↑ DB 건드리지 않고 빈 응답만 반환
                });
    }

    // 메뉴 담기
    // 이미 담긴 메뉴면 수량 누적, 없으면 새로 추가
    public CartDto addItem(Long userId, AddCartItemRequest request) {
        Cart cart = cartRepository.findByCustomerId(userId)
                .orElseGet(() -> cartRepository.save(Cart.createCart(userId)));

        Menu menu = menuRepository.findById(request.getMenuId())
                .filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new ServiceErrorException(ErrorEnum.NOT_FOUND_MENU));

        // 이미 담긴 메뉴면 수량 누적, 없으면 새로 추가
        cartItemRepository.findByCartIdAndMenuId(cart.getId(), menu.getId())
                .ifPresentOrElse(
                        existing -> existing.changeQuantity(existing.getQuantity() + request.getQuantity()),
                        () -> cartItemRepository.save(
                                CartItem.createCartItem(
                                        menu.getId(),
                                        menu.getName(),
                                        request.getQuantity(),
                                        menu.getPrice(),
                                        cart.getId()
                                )
                        )
                );
        log.info("[장바구니] 메뉴 추가 - userId={}, menuId={}, quantity={}",
                userId, menu.getId(), request.getQuantity());
        return buildCartDto(cart);
    }

    // 수량 변경 (0이면 해당 항목 삭제)
    public CartDto updateItemQuantity(Long userId, Long cartItemId, int quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ServiceErrorException(ErrorEnum.NOT_FOUND_CARTITEM));
        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.changeQuantity(quantity);
        }
        Cart cart = cartRepository.findByCustomerId(userId)
                .orElseThrow(() -> new ServiceErrorException(ErrorEnum.NOT_FOUND_CART));
        return buildCartDto(cart);
    }

    // 장바구니 전체 비우기 (물리 삭제)
    public void clearCart(Long userId) {
        cartRepository.findByCustomerId(userId).ifPresent(cart -> {
            cartItemRepository.deleteAllByCartId(cart.getId());
            cartRepository.delete(cart);
            log.info("[장바구니] 전체 비우기 - userId={}", userId);
        });
    }

    // 주문 완료 후 장바구니 + 아이템 물리 삭제
    // CartItem 먼저 삭제 후 Cart 삭제 (FK 순서)
    public void deleteCartAfterOrder(Long cartId) {
        cartItemRepository.deleteAllByCartId(cartId);
        cartRepository.deleteById(cartId);
        log.info("[장바구니] 주문 완료 후 삭제 - cartId={}", cartId);
    }

    // 내부 공통: Cart → CartDto 변환
    private CartDto buildCartDto(Cart cart) {
        List<CartItem> items = cartItemRepository.findAllByCartId(cart.getId());

        List<CartItemDto> itemDtos = items.stream()
                .map(CartItemDto::from)
                .toList();

        int totalPrice = itemDtos.stream()
                .mapToInt(CartItemDto::totalPrice)
                .sum();
        return new CartDto(cart.getId(), cart.getCustomerId(), itemDtos, totalPrice);
    }
}
