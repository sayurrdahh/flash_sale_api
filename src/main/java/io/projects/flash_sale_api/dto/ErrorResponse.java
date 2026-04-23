package io.projects.flash_sale_api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AllArgsConstructor, NoArgsConstructor 차이
 *
 * NoArgsConstructor : 아무런 매개변수(파라미터)를 받지 않는 빈 생성자를 자동으로 만들어줌
 * public CouponIssueRequest(){}
 *
 * AllArgsConstructor : 모든 파라미터를 받는 생성자
 * public ErrorResponse(String code, String message) {
 *         this.code = code;
 *         this.message = message;
 *     }
 * */

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;
}
