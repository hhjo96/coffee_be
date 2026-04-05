package com.example.coffee_be.domain.search.document;

import com.example.coffee_be.common.entity.Menu;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// @Document: jpa의 @Entity같은것.
@Document(indexName = "menus")
// @Setting: 인덱스 설정파일 연결. 노리분석기가 해당 위치에 있다는 의미
@Setting(settingPath = "elastic/menu-settings.json")
public class MenuDocument {

    @Id // mysql의 아이디를 그대로 사용하기도 하고, es는 원래 알아서 관리해준다고 함
    private Long id;

    // nori 형태소 분석기 적용
    // "아이스아메리카노" → "아이스", "아메리카노" 로 분리해서 색인
    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String name;

    @Field(type = FieldType.Integer)
    private int price;

    @Field(type = FieldType.Text)
    private String description;

    // AI 임베딩 벡터 (text-embedding-3-small = 1536차원)
    // openAI는 es를 db처럼 사용함
    @Field(type = FieldType.Dense_Vector, dims = 1536)
    private float[] embedding;

    // 임베딩 없이 저장(nori 검색)
    // Menu 엔티티 → MenuDocument 변환
    public static MenuDocument from(Menu menu) {
        return MenuDocument.builder()
                .id(menu.getId())
                .name(menu.getName())
                .price(menu.getPrice())
                .description(menu.getDescription())
                .build();
    }

    // 임베딩 포함해서 저장(ai검색)
    public static MenuDocument from(Menu menu, float[] embedding) {
        return MenuDocument.builder()
                .id(menu.getId())
                .name(menu.getName())
                .embedding(embedding)  // 벡터 포함
                .build();
    }
}
