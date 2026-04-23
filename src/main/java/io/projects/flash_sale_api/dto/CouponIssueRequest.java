package io.projects.flash_sale_api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 클ㄹ라이언트와 데이터를 주고받을 상자(DTO)
 * 컨트롤러에서 엔티티(Coupon, User 등)를 직접 노출하하는 것은 실무에서 금기사항
 * */

@Getter
@NoArgsConstructor
public class CouponIssueRequest {

    private Long userId; //실제로는 로그인 토큰(JWT)에서 추출

}
