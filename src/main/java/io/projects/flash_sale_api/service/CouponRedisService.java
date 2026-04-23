package io.projects.flash_sale_api.service;

import io.projects.flash_sale_api.domain.Coupon;
import io.projects.flash_sale_api.domain.CouponIssue;
import io.projects.flash_sale_api.repository.CouponIssueRepository;
import io.projects.flash_sale_api.repository.CouponRedisRepository;
import io.projects.flash_sale_api.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponRedisService {

    private final CouponRedisRepository couponRedisRepository;
    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;

    public void issue(Long couponId, Long userId){
        //1.쿠폰 마스터 정보 조회 (발급 가능한 최대 수량을 알기 위함)
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        //2. 레디스 Set으로 0(1) 중복 체크
        //RDB의 exists 쿼리를 날릴 필요 없이 메모리에서 즉시 차단됩니다
        Boolean isFirstRequest = couponRedisRepository.addIfAbsent(couponId, userId);
        if(Boolean.FALSE.equals(isFirstRequest)){
            throw new IllegalStateException("이미 발급받은 쿠폰입니다.");
        }

        //3.레디스 INCR로 원자적 수량 증가(DB락 불필요)
        Long count = couponRedisRepository.increment(couponId);

        //만약 101번째 요청이면 수량 초과 예외 발생
        if(count > coupon.getIssuedQuantity()){
            throw new IllegalStateException("준비된 쿠폰이 모두 소진되었습니다.");
        }

        //4.레디스 검증을 통과한 '진짜 당첨자' 100명만 RDB에 이중 저장
        //이전처럼 수만명이 DB문을 두드리지 않고, 100번의 INSERT만 발생
        couponIssueRepository.save(new CouponIssue(coupon, userId));

    }
}
