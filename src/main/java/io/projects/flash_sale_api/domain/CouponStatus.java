package io.projects.flash_sale_api.domain;
/**
 * 쿠폰의 상태를 관리할 enum 클래스
 * */

public enum CouponStatus {
    READY("발급 대기중"),
    ACTIVE("발급 진행중"),
    CLOSED("발급 종료(소진 또는 기한 만료)");

    private final String description;

    CouponStatus(String description){
        this.description = description;
    }
}
