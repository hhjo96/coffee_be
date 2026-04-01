package com.example.coffee_be.domain.point.service;

import com.example.coffee_be.common.entity.Point;
import com.example.coffee_be.common.entity.PointHistory;
import com.example.coffee_be.common.exception.ErrorEnum;
import com.example.coffee_be.common.exception.ServiceErrorException;
import com.example.coffee_be.domain.point.enums.PointStatus;
import com.example.coffee_be.domain.point.model.dto.PointDto;
import com.example.coffee_be.domain.point.model.request.ChargePointRequest;
import com.example.coffee_be.domain.point.repository.PointRepository;
import com.example.coffee_be.domain.pointHistory.repository.PointHistoryRepository;
import com.example.coffee_be.domain.pointHistory.service.PointHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.example.coffee_be.common.exception.ErrorEnum.POINT_CHARGE_CONFLICT;
import static com.example.coffee_be.common.exception.ErrorEnum.POINT_LOCK_FAILED;
import static com.example.coffee_be.domain.point.enums.PointStatus.CHARGED;
import static com.example.coffee_be.domain.point.enums.PointStatus.USED;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PointService {

    private final PointHistoryService historyService;
    private final PointRepository pointRepository;
    private final PointHistoryRepository historyRepository;

    private final RedissonClient redissonClient;

    // lock:point:user:{userId}
    private static final String LOCK_POINT_PREFIX = "lock:point:user:";
    // 1. 낙관적 락 — 충돌 시 최대 3회 재시도
    public PointDto chargePointsOpt(Long userId, ChargePointRequest req) {
        for (int i = 0; i < 3; i++) {
            try {
                log.info("[낙관적 락]" );
                return doCharge(userId, req);
            }
            catch (OptimisticLockingFailureException e) {
                if (i == 2) throw new ServiceErrorException(POINT_CHARGE_CONFLICT);
            }
        }
        throw new ServiceErrorException(POINT_CHARGE_CONFLICT);
    }

    // 2. 비관적 락(select for update)
    public PointDto chargePointsPes(Long userId, ChargePointRequest req) {

        Point point = pointRepository.findByCustomerIdWithPes(userId)
                .orElseGet(() -> pointRepository.save(Point.createPoint(userId)));
        int amount = req.getAmount();
        point.charge( amount);

        historyRepository.save(PointHistory.createPointHistory(userId,  amount, CHARGED, LocalDateTime.now()));

        log.info("[비관적 락]" );
        return new PointDto(userId, point.getBalance());
    }
    // 3. 분산 락 (Redisson RLock)
    public PointDto chargePointsDis(Long userId, ChargePointRequest req) {

        RLock lock = redissonClient.getLock(LOCK_POINT_PREFIX + userId);
        try {
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS))
                throw new ServiceErrorException(POINT_LOCK_FAILED);

            log.info("[분산 락] 포인트 충전 완료" );
            return doCharge(userId, req);

        } catch (InterruptedException e) {
            throw new ServiceErrorException(POINT_LOCK_FAILED);

        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }
    // 4. Lua 스크립트
    // 잔액을 읽어서 값이 없으면 -1(캐시에없다), 값이 차감할 금액보다 적으면 -2, 아니면 차감

    // 현재 레디스에 point:balance:{userId} 가 없으므로
    // todo: 충전 시 → DB 저장 + point:balance:{userId} Redis에도 씀 (캐시 세팅)
    // 주문 결제 시 → Lua로 Redis 잔액 사전 검증 → DB 비관적락으로 실제 차감
    private static final String DEDUCT_SCRIPT =
            "local b = tonumber(redis.call('GET', KEYS[1]))\n" +
                    "if b == nil then return -1 end\n" +
                    "if b < tonumber(ARGV[1]) then return -2 end\n" +
                    "return redis.call('DECRBY', KEYS[1], ARGV[1])";

    // 5. 최종선택: 분산락 + 비관적락으로 정합성 보장
    public PointDto usePointForOrder(Long userId, int price) {
        RLock lock = redissonClient.getLock(LOCK_POINT_PREFIX + userId);
        try {
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS))
                throw new ServiceErrorException(POINT_LOCK_FAILED);

            Point point = pointRepository.findByCustomerIdWithPes(userId)
                    .orElseThrow(() -> new ServiceErrorException(ErrorEnum.NOT_FOUND_POINT));

            point.use(price);

            historyRepository.save(PointHistory.createPointHistory(userId, price, USED, LocalDateTime.now()));

            return new PointDto(userId, point.getBalance());

        } catch (InterruptedException e) {
            throw new ServiceErrorException(POINT_LOCK_FAILED);

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private PointDto doCharge(Long userId, ChargePointRequest request) {
        Point point = pointRepository.findByCustomerId(userId)
                .orElseGet(() -> pointRepository.save(Point.createPoint(userId)));

        int amount = request.getAmount();

        point.charge(amount);

        PointHistory history = PointHistory.createPointHistory(userId, amount, PointStatus.CHARGED, LocalDateTime.now());
        historyRepository.save(history);

        log.info("포인트 충전 완료 - userId={}, balance={}", userId, point.getBalance());

        return new PointDto(userId, point.getBalance());
    }

}
