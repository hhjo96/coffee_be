package com.example.coffee_be.domain.menu.service;


import com.example.coffee_be.common.entity.Menu;
import com.example.coffee_be.common.exception.ErrorEnum;
import com.example.coffee_be.common.exception.ServiceErrorException;
import com.example.coffee_be.common.response.PageResponse;
import com.example.coffee_be.domain.menu.model.dto.MenuDto;
import com.example.coffee_be.domain.menu.model.request.UpdateMenuRequest;
import com.example.coffee_be.domain.menu.model.response.MenuCursorResponse;
import com.example.coffee_be.domain.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuCacheService menuCacheService;

    /// /////////////////////////////////////    전체 조회      ////////////////////////////////

    public PageResponse<MenuDto> getAllV1Projection(Pageable pageable) {
        Page<MenuDto> page =
                menuRepository.getAllV1(pageable);
        log.info("menu 전체 조회 v1");

        return new PageResponse<>(page);
    }

    public PageResponse<MenuDto> getAllV2DSL(Pageable pageable) {
        Page<MenuDto> page =
                menuRepository.getAllV2(pageable);
        log.info("menu 전체 조회 v2");

        return new PageResponse<>(page);
    }


    public MenuCursorResponse getAllV3Cursor(Long cursorId, int size) {
        log.info("menu 전체 조회 v3");
        return menuRepository.getAllV3(cursorId, size);
    }


    /// /////////////////////////////////////   단건 조회      ////////////////////////////////

    // 처음 호출시 db조회 후 레디스에 저장, 이후에는 레디스에서 바로 반환
    @Cacheable(value = "menu", key = "#menuId")
    public MenuDto getOneV1Cache(Long menuId) {
        Menu menu = menuRepository.findById(menuId).orElseThrow(() -> new ServiceErrorException(ErrorEnum.NOT_FOUND_MENU));
        log.info("menu 단건 조회 v1");

        return new MenuDto(menu.getId(), menu.getName(), menu.getPrice());
    }

    public MenuDto getOneV2Temp(Long menuId) {

        //레디스에 있나 확인
        MenuDto cached = menuCacheService.getPostCache(menuId);
        if(cached!= null) {
            return cached;
        }
        //없으면 db에서 가져오기
        Menu menu = menuRepository.findById(menuId).orElseThrow(() -> new ServiceErrorException(ErrorEnum.NOT_FOUND_MENU));

        // db에서 직접조회한 값을 캐시에 저장
        MenuDto dto = new MenuDto(menu.getId(), menu.getName(), menu.getPrice());
        menuCacheService.saveMenuCache(menuId, dto); // localdatetime 의 경우 여기서 에러

        log.info("menu 단건 조회 v2");

        return dto;

    }

    /// /////////////////////////////////////   수정     ////////////////////////////////

    //db 내용 수정시 삭제 어노테이션
    // 커밋 전 캐시 삭제로 옛날 값 캐싱하는 문제가 발생할 수 있으나, 수정하는 이벤트가 자주 일어나지 않으므로 별도 처리 없음
    @Transactional
    public MenuDto updatePostByIdV1(long postId, UpdateMenuRequest request) {
        Menu menu = menuRepository.findById(postId).orElseThrow(() -> new ServiceErrorException(ErrorEnum.NOT_FOUND_MENU));

        menu.changePrice(request.getPrice());

        //캐시 무효화
        evictMenuCache(menu.getId());

        menuCacheService.deleteMenuCache(menu.getId());
        return new MenuDto(menu.getId(), menu.getName(), menu.getPrice());
    }


    @CacheEvict(value = "menu", key = "#menuId")
    public void evictMenuCache(Long menuId) {
        // 메뉴 변경 시 캐시 무효화
    }

    //db 내용 수정시 캐시 삭제
    // 커밋 전 캐시 삭제로 옛날 값 캐싱하는 문제가 발생할 수 있으나, 수정하는 이벤트가 자주 일어나지 않으므로 별도 처리 없음
    @Transactional
    public MenuDto updatePostByIdV2(long menuId, UpdateMenuRequest request) {
        Menu menu = menuRepository.findById(menuId).orElseThrow(() -> new ServiceErrorException(ErrorEnum.NOT_FOUND_MENU));

        menu.changePrice(request.getPrice());

        //캐시 삭제
        menuCacheService.deleteMenuCache(menuId);

        return new MenuDto(menu.getId(), menu.getName(), menu.getPrice());
    }


    /// /////////////////////////////////////    삭제      ////////////////////////////////

    @Transactional
    public void delete(Long menuId) {
        boolean existence = menuRepository.existsById(menuId);
        if(!existence) {
            throw new ServiceErrorException(ErrorEnum.NOT_FOUND_MENU);
        }
        Menu menu = menuRepository.findById(menuId).orElseThrow(() -> new ServiceErrorException(ErrorEnum.NOT_FOUND_MENU));
        menu.deleteMenu();

    }
}
