package io.projects.flash_sale_api.service;

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
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CouponRedisServiceTest {

    @Autowired
    private CouponRedisService couponRedisService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponIssueRepository couponIssueRepository;

    //레디스 데이터를 검증하고 청소하기 위해 템플릿을 가져옴
    @Autowired
    private StringRedisTemplate redisTemplate;

    private Long savedCouponId;

    @BeforeEach
    void setUp() {
        //1. 테스트용 쿠폰 마스터 데이터 생성
        Coupon coupon = Coupon.builder()
                .name("레디스 선착순 100명 한정 쿠폰")
                .totalQuantity(100)
                .status(CouponStatus.ACTIVE)
                .startAt(LocalDateTime.now().minusDays(1))
                .endAt(LocalDateTime.now().plusDays(1))
                .build();
        savedCouponId = couponRepository.save(coupon).getId();

        //2. 가장 중요한 레디스 초기화
        //db와 다르게 레디스는 메모리에 데이터가 영구적으로 남기 때문에
        //테스트 시작 전에 이전 테스트의 key들을 확실히 날려줘야 함
        redisTemplate.delete("coupon:"+savedCouponId + ":users");
        redisTemplate.delete("coupon:"+savedCouponId+":count");
    }

    @AfterEach
    void tearDown() {
        //자식(발급내역)부터 지우고 부모(쿠폰)을 지운다.
        couponIssueRepository.deleteAll();
        couponRepository.deleteAll();
    }

    @Test
    @DisplayName("1000명의 유저가 동시에 요청하면 딱 100개만 발급되어야 한다.")
    void issueCouponConcurrently_Redis() throws InterruptedException{
        //given
        int threadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        //when
        for (int i = 0; i < threadCount; i++) {
            long userId = i;
            executorService.submit(() -> {
                try {
                    couponRedisService.issue(savedCouponId, userId);
                }catch (Exception e){

                }finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        //then
        //db에 실제로 저장된 발급 내역이 정확히 100건인지 확인
        long issueCount = couponIssueRepository.count();
        assertThat(issueCount).isEqualTo(100);

        //로그 출력: 레디스가 얼마나 많은 트래픽을 처리했는지 직접 눈으로 확인
        String finalRedisCount = redisTemplate.opsForValue().get("coupon:"+savedCouponId + ":count");
        System.out.println("issueCount 최종 db 발급 수량 = " + issueCount);
        System.out.println("finalRedisCount 레디스가 막아낸 총 오청 횟수 = " + finalRedisCount);

    }

    @Test
    @DisplayName("한 명의 유저가 마이크로초 단위로 100번을 광클해도 1개만 발급 ")
    void issueCoupon_DuplicateCheck() throws InterruptedException {
        //given
        int threadCount = 100;
        long sameUserId = 777L; //777번 유저 단 한명

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch =  new CountDownLatch(threadCount);

        //when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    //동일한 유저 아이디로 100번 동시 요청
                    couponRedisService.issue(savedCouponId, sameUserId);
                }catch (Exception e){
                    //중복 예외 무시
                }finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        //then
        //100번 요청 했지만 레디스의 set 덕분에 db에는 단 한건만 저장되어야 함
        long issueCount = couponIssueRepository.count();
        assertThat(issueCount).isEqualTo(1);

        System.out.println("광클 방어 테스트 최종 db 발급 수량 = " + issueCount);
    }

}