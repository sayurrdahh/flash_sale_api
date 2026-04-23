package io.projects.flash_sale_api.service;

import io.projects.flash_sale_api.domain.Coupon;
import io.projects.flash_sale_api.domain.CouponIssue;
import io.projects.flash_sale_api.repository.CouponIssueRepository;
import io.projects.flash_sale_api.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor //final이 붙은 필드 생성자를 자동 생성(의존성 주입)
@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;

    @Transactional
    public void issue(Long couponId, Long userId) {

        //1. 쿠폰 마스터 데이터 조회
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰 입니다."));

        //2. 이미 발급받은 유저인지 중복 체크
        if (couponIssueRepository.existsByCouponIdAndUserId(couponId, userId)) {
            throw new IllegalStateException("이미 발급 받은 쿠폰입니다.");
        }

        //3.발급 조건 검증 및 수량 차감 (Entity 내부의 비즈니스 로직 호출)
        // 객체 지향의 장점? : 서비스 계층이 깔끔해지고 상태 변경의 책임이 도메인에 집중됨
        coupon.issue(LocalDateTime.now());

        //4. 발급 내역(이력) 저장
        couponIssueRepository.save(new CouponIssue(coupon, userId));
    }
}
