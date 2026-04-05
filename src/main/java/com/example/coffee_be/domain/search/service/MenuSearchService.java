package com.example.coffee_be.domain.search.service;

import co.elastic.clients.elasticsearch._types.KnnSearch;
import com.example.coffee_be.domain.menu.model.dto.MenuDto;
import com.example.coffee_be.domain.search.document.MenuDocument;
import com.example.coffee_be.domain.search.repository.MenuSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuSearchService {

    private final MenuSearchRepository menuSearchRepository;
    // search 메서드를 쓰려면 필요함
    private final ElasticsearchOperations elasticsearchOperations;

    private final EmbeddingModel embeddingModel;  // Spring AI

    // initData에서 호출 — DB 메뉴 전체를 ES에 인덱싱
    public void saveAll(List<MenuDocument> documents) {
        menuSearchRepository.saveAll(documents);
        log.info("[ES] 메뉴 인덱싱 완료 - {}개", documents.size());
    }

    // nori + fuzzy 검색
    // nori  : 한국어 형태소 분석 — "아메리카노를" → "아메리카노" 매칭
    // fuzzy : 오타 허용 — "아메리가노" → "아메리카노" 매칭
    public List<MenuDto> searchByES(String keyword) {
        log.info("[ES] 메뉴 검색 - keyword={}", keyword);

        // 기본값 0~2글자는 일치, 3~5글자 짧은 단어는 편집거리 1, 6글자 이상 긴 단어는 2까지 허용한다고 함(현재 세팅한게 없으므로 기본값 유지)
        // "name" 필드에서 keyword 에 있는 걸로 검색하는 것
        Criteria criteria = new Criteria("name").fuzzy(keyword);

        // ES에 날릴 쿼리 만들기(점수 높은순 5개 자동정렬)
        Query query = new CriteriaQuery(criteria).setPageable(PageRequest.of(0, 5));

        // searchHits: 결과+score 목록
        SearchHits<MenuDocument> hits =
                elasticsearchOperations.search(query, MenuDocument.class);

        // 유사한 목록 리스트 반환_기본 10개
        return hits.getSearchHits().stream()
                .map(hit -> new MenuDto(
                        hit.getContent().getId(),
                        hit.getContent().getName(),
                        hit.getContent().getPrice()))
                .toList();
    }

    // 텍스트 → 1536차원 float 벡터 변환
    public float[] embed(String text) {

        return embeddingModel.embed(text);
    }

    // AI 임베딩 kNN 검색
    // nori+fuzzy: 텍스트 기반 — 오타 허용
    // kNN:        의미 기반 — "카페인 많은 거" → 에스프레소, 아메리카노
    public List<MenuDto> searchByEmbedding(String keyword) {
        float[] queryVector = embed(keyword);

        // float[] → List<Float> 변환
        List<Float> vectorList = new ArrayList<>();
        for (float v : queryVector) vectorList.add(v);

        Query knnQuery = NativeQuery.builder()
                .withKnnSearches(
                        KnnSearch.of(k -> k
                                .field("embedding")
                                .queryVector(vectorList)
                                .numCandidates(50)
                                .k(5)
                        )
                )
                .build();

        SearchHits<MenuDocument> hits =
                elasticsearchOperations.search(knnQuery, MenuDocument.class);

        return hits.getSearchHits().stream()
                .map(hit -> new MenuDto(
                        hit.getContent().getId(),
                        hit.getContent().getName(),
                        hit.getContent().getPrice()))
                .toList();
    }
}
