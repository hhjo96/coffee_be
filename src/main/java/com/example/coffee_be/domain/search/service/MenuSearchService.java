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

    // nori + fuzzy 검색 + boost 3단계 스코어링
    // nori  : 한국어 형태소 분석 — "아메리카노를" → "아메리카노" 매칭
    // fuzzy : 오타 허용 — "아메리가노" → "아메리카노" 매칭
    public List<MenuDto> searchByES(String keyword) {
        log.info("[ES] 메뉴 검색 - keyword={}", keyword);
        // "name" 필드에서 keyword 에 있는 걸로 검색하는 것

/*  이렇게 치면 json 축약어를 안 탄다!! 참고용으로 남겨둠
        Criteria criteria = new Criteria("name").fuzzy(keyword);

        // ES에 날릴 쿼리 만들기(점수 높은순 5개 자동정렬)
        Query query = new CriteriaQuery(criteria).setPageable(PageRequest.of(0, 5));

 */

        /*
         *     1 match_phrase  boost=3.0 : 구문 완전 일치       최우선 — "아이스 아메리카노" 그대로 입력한 경우
               2 match(nori)   boost=2.0 : 형태소/동의어 분석   차우선 — "아아" → "아이스아메리카노" 매칭
               3 fuzzy         boost=0.5 : 오타 허용 편집거리   최하위 — "아메리가노" → "아메리카노" 매칭
         *
         * 1, 2, 3을 합산
         */

        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .should(s -> s
                                        // 1 구문 완전 일치 — 가장 정확한 검색. boost 최고
                                        .matchPhrase(mp -> mp
                                                .field("name")
                                                .query(keyword)
                                                .boost(3.0f)
                                        )
                                )
                                .should(s -> s
                                        // 2 동의어 검색용 (아아 → 아이스아메리카노)
                                        .match(m -> m
                                                .field("name")
                                                .query(keyword)
                                                .analyzer("nori_analyzer")
                                                .boost(2.0f)
                                        )
                                )
                                .should(s -> s
                                        // fuzzy 검색용 (아메리가노 → 아메리카노)
                                        // AUTO: 0~2글자 완전일치, 3~5글자 편집거리 1, 6글자이상 편집거리 2
                                        .fuzzy(f -> f
                                                .field("name")
                                                .value(keyword)
                                                .fuzziness("AUTO")
                                                .boost(0.5f)
                                        )
                                )
                        )
                ) // or 검색
                .withMinScore(1.0f)
                .withPageable(PageRequest.of(0, 5))
                .build();

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
        for (float v : queryVector) {
            vectorList.add(v);
        }

        Query knnQuery = NativeQuery.builder()
                .withKnnSearches(
                        KnnSearch.of(k -> k
                                .field("embedding")
                                .queryVector(vectorList)
                                .numCandidates(30) // 후보 30개
                                .k(5) //진짜 가까운 5개
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
