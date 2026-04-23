package io.projects.flash_sale_api.service.facade;

import io.projects.flash_sale_api.domain.Coupon;
import io.projects.flash_sale_api.domain.CouponStatus;
import io.projects.flash_sale_api.repository.CouponIssueRepository;
import io.projects.flash_sale_api.repository.CouponRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ExecutorService(스레드 풀 - 인력사무소)
 * - 미리 정해진 수의 일꾼(스레드)만 고용해 두고, 일거리를 큐(Queue)에 쌓아두는 방식
 * - ExecutorService executorService = Executors.newFixedThreadPool(32);
 *  32명의 일꾼만 고용하는 인력 사무소 설립
 *
 * CountDownLatch (스레드 동기화)
 * - 여러 스레드의 작업이 모두 끝날 때까지 특정 스레드(보통 메인 스레드)를 대기시키는 역할
 * - 숫자를 카운트다운하는 잠금장치(latch)를 만듦
 */

@SpringBootTest //스프링 컨테이너를 띄워 실제 db까지 연동하는 통합 테스트
class CouponOptimisticLockFacadeTest {

    @Autowired
    private CouponOptimisticLockFacade couponOptimisticLockFacade;

    @Autowired
    private CouponRepository couponRepository;

    //발급 내역을 지우기 위해 레포지토리 추가
    @Autowired
    private CouponIssueRepository couponIssueRepository;

    private Long savedCouponId;

    @BeforeEach
    void setUp() {
        //테스트 시작 전 : 총 수량이 100개인 쿠폰 생성 db 저장
        Coupon coupon = Coupon.builder()
                .name("선착순 100명 한정 쿠폰")
                .totalQuantity(100)
                .status(CouponStatus.ACTIVE)
                .startAt(LocalDateTime.now().minusDays(1))
                .endAt(LocalDateTime.now().plusDays(1))
                .build();

        savedCouponId = couponRepository.save(coupon).getId();
    }

    @AfterEach
    void tearDown() {
        //다음 테스트에 영향ㅇ르 주지 않도록 데이터 정리

        //자식 테이블(발급 내역) 데이터를 먼저 전부 삭제
        couponIssueRepository.deleteAll();
        //그 다음 부모 테이블(쿠폰) 데이터를 삭제
        couponRepository.deleteAll();
    }

    @Test
    @DisplayName("1000명의 유저가 동시 요청, 쿠폰은 100개만 발급되어야 함")
    void issue() throws InterruptedException{
        //given
        int threadCount = 1000; //동시에 요청을 보낼 유저(스레드) 수

        //비동기로 실행할 작업을 관리하는 스레드 풀 (32개의 스레드가 번갈아가며 일함)
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        //1000개의 스레드가 모두 끝날 때까지 메인 스레드가 기다리도록 하는 안전장치
        CountDownLatch latch = new CountDownLatch(threadCount);

        //when
        for (int i = 0; i < threadCount; i++) {
            long userId = i; //0번부터 999번까지 가상의 유저 id

            executorService.submit(() -> {
                try {
                   //Facade 메서드 호출 (낙관적 락 재시도 로직 포함)
                    couponOptimisticLockFacade.issue(savedCouponId, userId);
                }catch (Exception e){
                    //재고 소진 등의 예외가 발생해도 테스트 진행
                }finally {
                    //성공하든 실패하든 스레드 작업이 끝나면 카운트 1 감소
                    latch.countDown();
                }
            });
        }

        //1000개의 작업이 모두 끝날때까지(카운트 0 될 때 까지) 메인 스레드 대기
        latch.await();

        //then
        Coupon findCoupon = couponRepository.findById(savedCouponId).orElseThrow();

        //동시성 제어가 완벽하다면, 1000명이 요청했어도 발급된 수량은 정확히 100이어야 함 
        assertThat(findCoupon.getIssuedQuantity()).isEqualTo(100);

        System.out.println("findCoupon.getIssuedQuantity() = " + findCoupon.getIssuedQuantity());
    }
}