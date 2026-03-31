package com.example.coffee_be.domain.menu.service;


import com.example.coffee_be.domain.menu.model.dto.MenuDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MenuCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_POST_PREFIX = "menu:";
    private static final String RANKING_POST_KEY = "ranking:menus";


    // 캐시를 조회하는 기능
    // 그냥 리턴하면 object 타입으로 리턴
    public MenuDto getPostCache(long menuId){
        String key = CACHE_POST_PREFIX + menuId;
        return (MenuDto) redisTemplate.opsForValue().get(key);
    }

    // 캐시를 저장하는 기능
    public void saveMenuCache(long postId, MenuDto dto) {
        String key = CACHE_POST_PREFIX + postId;
        redisTemplate.opsForValue().set(key, dto, 20, TimeUnit.MINUTES);

    }

    // 캐시를 삭제하는 기능
    public void deleteMenuCache(long menuId) {
        String key = CACHE_POST_PREFIX + menuId;
        redisTemplate.delete(key);

    }

//    //조회된 게시글의 조회수 증가
//    public void increateViewCount(Long menuId) {
//        redisTemplate.opsForZSet().incrementScore(RANKING_POST_KEY, menuId.toString(), 1);
//
//    }
//
//    //인기 게시글 조회
//    public List<Long> getTopPostList(int limit) {
//        Set<Object> menuIdList = redisTemplate.opsForZSet().reverseRange(RANKING_POST_KEY, 0, limit-1);
//
//        if(menuIdList == null || menuIdList.isEmpty()) {
//            return Collections.emptyList();
//        }
//        //set object 타입을 list long으로
//        return menuIdList.stream().map(id -> Long.parseLong(id.toString())).toList();
//    }
}
