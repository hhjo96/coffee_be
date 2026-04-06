package com.example.coffee_be.domain.search.controller;

import com.example.coffee_be.common.response.BaseResponse;
import com.example.coffee_be.domain.menu.model.dto.MenuDto;
import com.example.coffee_be.domain.search.service.MenuSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menus")
public class MenuSearchController {

    private final MenuSearchService menuSearchService;

    // ES를 활용한 유사어 검색(nori 형태소 분석 + fuzzy)
    @GetMapping("/search/es")
    public ResponseEntity<BaseResponse<List<MenuDto>>> searchByEs(@RequestParam String q) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success("200", "ES 메뉴 검색 성공",
                menuSearchService.searchByES(q)));
    }

    // AI 임베딩 검색 (의미 기반 kNN)
    @GetMapping("/search/ai")
    public ResponseEntity<BaseResponse<List<MenuDto>>> searchByAi(@RequestParam String q) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success("200", "AI 메뉴 검색 성공",
                        menuSearchService.searchByEmbedding(q)));
    }

}
