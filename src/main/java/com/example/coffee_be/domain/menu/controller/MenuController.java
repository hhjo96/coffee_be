package com.example.coffee_be.domain.menu.controller;

import com.example.coffee_be.common.entity.Menu;
import com.example.coffee_be.common.response.BaseResponse;
import com.example.coffee_be.common.response.PageResponse;
import com.example.coffee_be.domain.menu.model.dto.MenuDto;
import com.example.coffee_be.domain.menu.model.request.UpdateMenuRequest;
import com.example.coffee_be.domain.menu.model.response.MenuCursorResponse;
import com.example.coffee_be.domain.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    // 다양한 방법으로 메뉴 조회 구현(전체조회 3개 및 단건조회 2개)

    // 목록: 그냥 프로젝션
    @GetMapping("/v1")
    public ResponseEntity<BaseResponse<PageResponse<MenuDto>>> getAllV1Projection(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success("200", "메뉴 목록 조회: 1 성공", menuService.getAllV1Projection(pageable)));
    }

    // 목록: 쿼리dsl + 페이징
    @GetMapping("/v2")
    public ResponseEntity<BaseResponse<PageResponse<MenuDto>>> getAllV2DSL(Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success("200", "메뉴 목록 조회: 2 성공", menuService.getAllV2DSL(pageable)));
    }

    // 목록: 커서
    @GetMapping("/v3")
    public ResponseEntity<BaseResponse<MenuCursorResponse>> getAllV3Cursor(
            @RequestParam(required = false) Long cursorId, @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success("200", "메뉴 목록 조회: 3 성공", menuService.getAllV3Cursor(cursorId, size)));
    }

    // todo: 단건1의 캐싱, 단건2의 캐싱이 동작하지 않고 있다! 확인필요
    // 단건: @Cacheable
    @GetMapping("/{menuId}/v1")
    public ResponseEntity<BaseResponse<MenuDto>> getOneV1Cacheable(@PathVariable Long menuId) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success("200", "메뉴 단건 조회: 1 성공", menuService.getOneV1Cache(menuId)));
    }

    // 단건: 레디스템플릿
    @GetMapping("/{menuId}/v2")
    public ResponseEntity<BaseResponse<MenuDto>> getOneV2RedisTemplate(@PathVariable Long menuId, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success("200", "메뉴 단건 조회: 2 성공", menuService.getOneV2Temp(menuId)));
    }

    // todo: 수정시 메뉴의 존재여부 확인 필요

    // 수정: @Cacheable
    //수정시 캐시 삭제
    @PatchMapping("/{menuId}/v1")
    public ResponseEntity<BaseResponse<MenuDto>> updateMenuByIdV2(@PathVariable Long menuId, @RequestBody UpdateMenuRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success("200", "메뉴 수정: 1 성공",menuService.updatePostByIdV2(menuId, request)));
    }

    // 수정: 레디스템플릿
    //수정시 캐시 삭제
    @PatchMapping("/{menuId}/v2")
    public ResponseEntity<BaseResponse<MenuDto>> updateMenuByIdV1(@PathVariable Long menuId, @RequestBody UpdateMenuRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success("200", "메뉴 수정: 2 성공",menuService.updatePostByIdV1(menuId, request)));
    }

    // 삭제: 소프트딜리트
    @DeleteMapping("/{menuId}")
    public ResponseEntity<BaseResponse<MenuDto>> delete(@PathVariable Long menuId) {
        menuService.delete(menuId);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success("200", "메뉴 삭제 성공", null));
    }



//    //인기 게시글 조회
//    @GetMapping("/popular")
//    public ResponseEntity<List<MenuDto>> getPopularPostList(@RequestParam(defaultValue = "10") int limit) {
//        return ResponseEntity.ok(menuService.getTopePostList(limit));
//    }

}
