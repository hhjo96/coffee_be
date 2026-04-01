package com.example.coffee_be.domain.point.controller;

import com.example.coffee_be.common.response.BaseResponse;
import com.example.coffee_be.common.response.PageResponse;
import com.example.coffee_be.domain.menu.model.dto.MenuDto;
import com.example.coffee_be.domain.point.model.dto.PointDto;
import com.example.coffee_be.domain.point.model.request.ChargePointRequest;
import com.example.coffee_be.domain.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;

    // 충전 및 잔액확인 - 낙관적 락
    @PatchMapping("/{userId}/opt")
    public ResponseEntity<BaseResponse<PointDto>> chargePointsOpt(@PathVariable Long userId, @RequestBody ChargePointRequest request) {

        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success("200", "포인트 충전 성공", pointService.chargePointsOpt(userId, request)));
    }

    // 충전 및 잔액확인 - 비관적 락
    @PatchMapping("/{userId}/pes")
    public ResponseEntity<BaseResponse<PointDto>> chargePointsPes(@PathVariable Long userId, @RequestBody ChargePointRequest request) {

        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success("200", "포인트 충전 성공", pointService.chargePointsPes(userId, request)));
    }

    // 충전 및 잔액확인 - 분산 락
    @PatchMapping("/{userId}/dis")   // ← 추가
    public ResponseEntity<BaseResponse<PointDto>> chargePointsDis(@PathVariable Long userId, @RequestBody ChargePointRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success("200", "포인트 충전 성공",
                        pointService.chargePointsDis(userId, request)));
    }


    // 사용 및 잔액 확인
}
