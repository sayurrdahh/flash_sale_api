package io.projects.flash_sale_api.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * JPA Auditing : 데이터베이스에 데이터가 입력되거나 수정될 때, 그 기록을 자동으로 남겨주는 기능
 * 객체가 생성되거나 수정되는 시점을 JPA가 가로채서 자동으로 시간을 채워줌 .
 *
 * @EntityListeners : JPA 엔티티의 라이프사이클 이벤트를 감지하고, 특정 시점에 원하는 로직을 자동으로 실행하게 해줌
 *
 * */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) //기본 생성자 접근 제어로 객체 생성 안전성 확보
@EntityListeners(AuditingEntityListener.class) //JPA Auditing 활성화 (생성/수정 시간 자동화)
@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer totalQuantity = 0 ; //총 발급 가능한 수량

    @Column(nullable = false)
    private Integer issuedQuantity = 0; //현재 발급된 수량
    
    /**
     * @Enumerated :
     * Enum(열거형) 데이터를 데이터베이스에 어떻게 저장할지 결정해주는 JPA 어노테이션
     * EnumType.ORDINAL : Enum 이 정의된 순서(숫자)를 저장 (0,1,2)
     * EnumType.STRING : Enum 이름 그 자체를 db에 저장(READY, ACTIVE ..)
     * */
    @Enumerated(EnumType.STRING) //Enum 이름을 db에 그대로 저장
    @Column(nullable = false, length = 20)
    private CouponStatus status = CouponStatus.READY; //쿠폰 상태

    @Column(nullable = false)
    private LocalDateTime startAt; //발급 시작 일시

    @Column(nullable = false)
    private LocalDateTime endAt; //발급 마감 일시

    /**
     * @Version은 동시성 제어의 핵심 기술인 낙관적 락 Optivistic Lock 을 구현하는 JPA 어노테이션
     * 데이터베이스 테이블에 version이라는 숫자 컬럼을 하나 추가하고
     * 데이터를 수정할 때마다 이 숫자를 1씩 증가시킴
     * UPDATE coupon SET quantity = 0, version = 2 WHERE id = 1 AND version = 1;
     * 업데이트된 행(row)이 0개라는 것을 확인하는 순간 예외 터짐
     *
     * 비관적 락(Pessimistic Lock) 은 무조건 충돌이 날거라고 비관적으로 생각해서
     * A가 데이터를 읽는 순간부터 다른 사람이 아예 접근하지 못하게 DB 레코드에 자물쇠를 걸어버려서 DB 성능이 크게 떨어짐
     *
     * 낙관적 락은 업데이트 시점에 '내가 읽었던 버전이 아직 그대로인지' 확인만 하는 방식
     * */
    @Version //범용적 기술 어필 : 낙관적 락(Optimistic lock)을 위한 버전 관리 필드
    private Long version;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt; //등록일자

    @LastModifiedDate
    private LocalDateTime updatedAt; //수정일자

    /**
     * 객체 지향적 설계를 위한 비즈니스 로직(Entity 내부에 응집)
    */
    //발급 가능한 상태인지 시간과 수량을 모두 검증
    public boolean isIssueable(LocalDateTime now){
        return this.status == CouponStatus.ACTIVE &&
                now.isAfter(this.startAt) &&
                now.isBefore(this.endAt) &&
                this.totalQuantity > this.issuedQuantity;
    }

    //쿠폰 발급 처리 (수량 증가 및 소진 시 상태 변경)
    public void issue(LocalDateTime now) {
        if(!isIssueable(now)){
            throw new IllegalStateException("현재 쿠폰을 발급할 수 없는 상태입니다.");
        }

        this.issuedQuantity++;

        //방금 발급으로 수량 소진됐으면 closed 변경
        if(this.issuedQuantity.equals(this.totalQuantity)){
            this.status = CouponStatus.CLOSED;
        }

    }

    @Builder
    public Coupon(String name, Integer totalQuantity, LocalDateTime startAt, LocalDateTime endAt, CouponStatus status){
        this.name = name;
        this.totalQuantity = totalQuantity;
        this.startAt = startAt;
        this.endAt = endAt ;
        this.issuedQuantity = 0; //초기값
        this.status = status;
    }
    
}
