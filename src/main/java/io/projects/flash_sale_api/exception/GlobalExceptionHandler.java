package io.projects.flash_sale_api.exception;

import io.projects.flash_sale_api.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice //전역 컨트롤러 예외 처리기
public class GlobalExceptionHandler {

    //1. 잘못된 입력값 처리(예 : 존재하지 않는 쿠폰)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e){
        log.warn("잘못된 요청 : {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("BAD_REQUEST",e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse); //400에러 발생
    }

    //2. 비즈니스 상태 충돌 처리 (예 : 중복 발급, 쿠폰 소진, 기간 아님)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e){
        log.warn("상태 충돌: {}", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("CONFLICT", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse); // 409 error
    }

    //3. 우리가 예상하지 못한 모든 에러 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e){
        log.error("서버 내부 오류 발생",e); //500에러는 반드시 로그 에러의 전체 원인을 남겨야함
        ErrorResponse errorResponse = new ErrorResponse("INTERNAL_SERVER_ERROR","서버 오류 발생");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse); //500 에러 반환
    }
}
