package com.example.coffee_be.concurrencyTest;

import com.example.coffee_be.common.entity.Point;
import com.example.coffee_be.domain.point.model.request.ChargePointRequest;
import com.example.coffee_be.domain.point.repository.PointRepository;
import com.example.coffee_be.domain.point.service.PointService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

// userId=1 한 명이 100개 요청 동시에 할 때 포인트 정확성 확인(실제로는 잘 일어나지 않겠지만)
@SpringBootTest
@Slf4j
@ActiveProfiles("test")
class ConcurrencyTest {

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private PointService pointService;

    private static final Long USER_ID = 999L;
    private static final int THREAD_COUNT = 100;
    private static final int CHARGE_AMOUNT = 1000;
    private static final int INITIAL_BALANCE = 100_000; // 숫자 가독성에 콤마 대신 _ 사용가능함

    @BeforeEach
    void setUp() {
        // 포인트 이미 있다면 삭제
        pointRepository.findByCustomerId(USER_ID).ifPresent(pointRepository::delete);
        // 초기 잔액 100,000원 세팅
        Point point = Point.createPoint(USER_ID);
        point.charge(INITIAL_BALANCE);
        pointRepository.saveAndFlush(point);
    }

    /// ///////////////// 정확성 비교    /// /////////////////
    @Test
    @DisplayName("낙관적 락 - 100개 동시 충전 (재시도 3회)")
    void testOptimisticLock() throws InterruptedException {
        AtomicInteger successCount = runAccuracy(() -> pointService.chargePointsOpt(USER_ID, new ChargePointRequest(CHARGE_AMOUNT)));

        // 성공한 만큼만 충전됐는지 확인
        Point point = pointRepository.findByCustomerId(USER_ID).orElseThrow();
        int expectedBalance = INITIAL_BALANCE + (successCount.get() * CHARGE_AMOUNT);
        log.info("성공: {}건", successCount.get());
        assertThat(point.getBalance()).isEqualTo(expectedBalance);
    }

    @Test
    @DisplayName("비관적 락 - 100개 동시 충전 (SELECT FOR UPDATE)")
    void testPessimisticLock() throws InterruptedException {
        // 비관적 락은 모두 성공해야 함

        AtomicInteger successCount = runAccuracy(() -> pointService.chargePointsPes(USER_ID, new ChargePointRequest(CHARGE_AMOUNT)));

        Point point = pointRepository.findByCustomerId(USER_ID).orElseThrow();
        int expectedBalance = INITIAL_BALANCE + (THREAD_COUNT * CHARGE_AMOUNT);
        log.info("성공: {}건", successCount.get());
        assertThat(successCount.get()).isEqualTo(THREAD_COUNT);
        assertThat(point.getBalance()).isEqualTo(expectedBalance);

        assertThat(successCount.get()).isEqualTo(THREAD_COUNT);
        assertThat(point.getBalance()).isEqualTo(200_000); // 100,000 + 100*1,000
    }

    @Test
    @DisplayName("분산 락 - 100개 동시 충전 (Redisson RLock)")
    void testDistributedLock() throws InterruptedException {

        AtomicInteger successCount = runAccuracy(
                () -> pointService.chargePointsDis(USER_ID, new ChargePointRequest(CHARGE_AMOUNT)));

        Point point = pointRepository.findByCustomerId(USER_ID).orElseThrow();
        int expectedBalance = INITIAL_BALANCE + (successCount.get() * CHARGE_AMOUNT);

        log.info("성공: {}건, 실패: {}건", successCount.get(), THREAD_COUNT - successCount.get());
        assertThat(point.getBalance()).isEqualTo(expectedBalance); // 성공한 만큼만 충전됐는지 확인. 코드에서 5초 시간제한 있어서 많이 실패함
    }

    @Test
    @DisplayName("최종 선택 - 100개 동시 포인트 차감 (분산락+비관적락)")
    void testUsePointForOrder() throws InterruptedException {

        AtomicLong orderIdCounter = new AtomicLong(0);
        AtomicInteger successCount = runAccuracy(
                () -> pointService.usePointForOrder(USER_ID, CHARGE_AMOUNT, orderIdCounter.incrementAndGet()));

        Point point = pointRepository.findByCustomerId(USER_ID).orElseThrow();
        int expectedBalance = INITIAL_BALANCE - (successCount.get() * CHARGE_AMOUNT);
        assertThat(point.getBalance()).isEqualTo(expectedBalance);
        assertThat(point.getBalance()).isGreaterThanOrEqualTo(0);
    }

    // 공통 내용 빼기
    private AtomicInteger runAccuracy(Runnable task) throws InterruptedException {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final long orderId = i + 1L;
            executor.submit(() -> {
                try {
                    task.run();
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally { latch.countDown(); }
            });
        }
        latch.await();
        executor.shutdown();

        return successCount;
    }


    /// ///////////////// 성능 비교    /// /////////////////

    @Test
    @DisplayName("락 방식별 성능 비교")
    void testLockPerformanceComparison() throws InterruptedException {
        setUp();
        long s1 = System.currentTimeMillis();
        AtomicInteger optSuccess = runAccuracy(
                () -> pointService.chargePointsOpt(USER_ID, new ChargePointRequest(CHARGE_AMOUNT)));
        long opt = System.currentTimeMillis() - s1;

        setUp();
        long s2 = System.currentTimeMillis();
        AtomicInteger pesSuccess = runAccuracy(
                () -> pointService.chargePointsPes(USER_ID, new ChargePointRequest(CHARGE_AMOUNT)));
        long pes = System.currentTimeMillis() - s2;

        setUp();
        long s3 = System.currentTimeMillis();
        AtomicInteger disSuccess = runAccuracy(
                () -> pointService.chargePointsDis(USER_ID, new ChargePointRequest(CHARGE_AMOUNT)));
        long dis = System.currentTimeMillis() - s3;

        // 다른 락은 충전이고 얘는 차감이라 기능의 의미가 좀 다르지만 락 테스트이므로 같이 넣었음
        setUp();
        AtomicLong orderIdCounter = new AtomicLong(0);
        long s4 = System.currentTimeMillis();
        AtomicInteger useSuccess = runAccuracy(
                () -> pointService.usePointForOrder(USER_ID, CHARGE_AMOUNT, orderIdCounter.incrementAndGet()));
        long use = System.currentTimeMillis() - s4;

        log.info("========== 락 방식별 성능 비교 (100 스레드) ==========");
        log.info("낙관적 락 : {}ms, 성공: {}건, 실패: {}건", opt, optSuccess.get(), THREAD_COUNT - optSuccess.get());
        log.info("비관적 락 : {}ms, 성공: {}건, 실패: {}건", pes, pesSuccess.get(), THREAD_COUNT - pesSuccess.get());
        log.info("분산 락   : {}ms, 성공: {}건, 실패: {}건", dis, disSuccess.get(), THREAD_COUNT - disSuccess.get());
        log.info("최종(분산+비관적) : {}ms, 성공: {}건, 실패: {}건", use, useSuccess.get(), THREAD_COUNT - useSuccess.get());
        log.info("====================================================");
    }
}
