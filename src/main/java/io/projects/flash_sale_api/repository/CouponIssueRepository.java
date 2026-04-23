package io.projects.flash_sale_api.repository;

import io.projects.flash_sale_api.domain.CouponIssue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

    //특정 유저가 특정 쿠폰을 이미 발급 받았는지 확인(중복 발급 방지용)
    //exists 쿼리를 사용하여 데이터 존재 여부만 빠르게 확인
    boolean existsByCouponIdAndUserId(Long couponId, Long userId);

}
