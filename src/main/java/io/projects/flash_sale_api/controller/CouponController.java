package io.projects.flash_sale_api.controller;

import io.projects.flash_sale_api.dto.CouponIssueRequest;
import io.projects.flash_sale_api.service.CouponRedisService;
import io.projects.flash_sale_api.service.facade.CouponOptimisticLockFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 1. 관심사의 분리(SoC)
 * 컨트롤러 내부를 try-catch로 작성하지 않고
 * 비즈니스 로직(Service/Facade),요청 처리(Controller),예외 처리(Advice)가
 * 각자 역할만 완벽하게 수행하도록 설계
 *
 * 2. 클라이언트 친화적 설계
 * 프론트엔드 개발자나 앱 개발자가 에러 상황을 명확하게 인지하고 사용자에게
 * 얼럿을 띄울 수 있도록 HttpStatus 코드 (400,409, 500)와 공통(ErrorResponse)규격을 제공
 *
 * 3. 로깅 전략
 * 에러 발생시 log.warn 과 log.error를 구분하여 남겼다.
 * 사용자의 잘못된 요청(400번대)은 warn으로 가볍게 남기고
 * 진짜 오류(500) error 로 전체 스택 트레이스를 남겨 추후 장애 추적이 가능하도록 구성
 *
 * v1 과 v2 둘 다 쓴 이유
 * - 비교 테스트 가능 (부하 테스트): 나중에 Jmeter나 nGrinder 같은 부하 테스트 툴을 사용해서 /v1/... API와 /v2/... API에 각각 똑같이 1만 명의 트래픽을 쏴볼 수 있습니다.
 * - 명확한 성능 지표 획득: "V1(DB락)은 1만 명 처리 시 응답 속도 평균 2초, CPU 90%였으나, V2(Redis)로 개선 후 응답 속도 0.05초, CPU 20%로 성능을 극적으로 향상시켰습니다"라는 수치화된 포트폴리오 스펙을 뽑아낼 수 있습니다.
 * - API 버저닝 이해도 어필: 실무에서 서비스 장애 없이 로직을 개편할 때 사용하는 /v1, /v2 버저닝 방식을 이해하고 있다는 것을 자연스럽게 보여줄 수 있습니다.
 *
 * */

@RestController
@RequestMapping("/api") //공통 경로
@RequiredArgsConstructor
public class CouponController {

    //1. 기존 db 낙관적 락 방식
    private final CouponOptimisticLockFacade couponOptimisticLockFacade;

    //2. 개선된 redis 방식
    private final CouponRedisService couponRedisService;

    /**
     * v1. RDB 낙관적 락을 이용한 발급 API(기존 방식)
     * */
    @PostMapping("/v1/coupon/{couponId}/issue")
    public ResponseEntity<Void> issueCoupon(
            @PathVariable Long couponId,
            @RequestBody CouponIssueRequest request
            ) throws InterruptedException{
        //비즈니스 로직(Facade)호출
        couponOptimisticLockFacade.issue(couponId, request.getUserId());

        //성공시 200 ok 응답 (바디 없음)
        return ResponseEntity.ok().build();
    }

    /**
     * v2. redis 메모리를 이용한 초고속 발급 API (개선된 방식)
     * */
    @PostMapping("/v2/coupons/{couponId}/issue")
    public ResponseEntity<Void> issueCouponV2(
            @PathVariable Long couponId,
            @RequestParam Long userId
    ){
        couponRedisService.issue(couponId, userId);
        return ResponseEntity.ok().build();

        /**
         * ResponseEntity(택배 상자)
         * .ok() 상태 코드 설정
         * .build() 포장 완료 : 본문 데이터 없이 깔끔하게 상태코드만 가진 응답 객체를 최종적으로 완성함.
         * */
    }
}
