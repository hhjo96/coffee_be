package com.example.coffee_be.domain.search.repository;

import com.example.coffee_be.domain.search.document.MenuDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface MenuSearchRepository extends ElasticsearchRepository<MenuDocument, Long> {
}
