package io.projects.flash_sale_api.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "coupon_issues",
        //핵심 범용 기술 : db레벨에서 1인당 1회 발급을 보장하는 유니크 인덱스
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_coupon_id_user_id",
                        columnNames = {"coupon_id", "user_id"}
                )
        }
)
public class CouponIssue {

    /**
     * @GeneratedValue(strategy = GenerationType.IDENTITY)
     * 기본키(PK)생성을 데이터베이스에 전적으로 맡긴다는 설정
     * MySQL : AUTO_INCREMENT
     * SQL Server/PostgreSQL : IDENTITY
     * 개발자가 id값을 null로 비워두고 save()를 호출하면, db가 알아서 다음 번호 생성
     * */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ManyToOne : 여러 개의 발급 내역(CouponIssue)이 하나의 쿠폰(Coupon)을 참조한다는 N:1 관계
     *
     * fetch = FetchType.LAZY (지연 로딩)
     * : CouponIssue를 조회할 때, 실제로 사용하기 전까지는 연결된 Coupon정보를 db에서 가져오지 않음
     * 대신 가짜 객체 프록시(Proxy)를 끼워둔다.
     * 나중에 issue.getCoupon().getName() 처럼 실제 데이터가 필요한 시점에 그제서야 DB에 쿼리를 날려 데이터를 가져옴
     *
     * 기본값 EAGER(즉시 로딩)을 쓰면 발급 내역 100건을 조회할 때, 원치 않은 쿠폰 정보까지 가져오기 위해
     * 100번의 추가 쿼리가 나갈 수 있다. 실무에서는 수만건의 데이터를 다루기 때문에 시스템 성능을 위해
     * 모든 연관 관계는 일단 LAZY로 설정
     *
     * JoinColumn : 객체와 테이블의 외래키(FK)를 매핑하고 제약 조건을 거는 역할
     * name = "coupon_id" : 데이터베이스의 coupon_issues테이블에 생성될 외래키 컬럼의 이름을 지정
     * */
    @ManyToOne(fetch = FetchType.LAZY) // 실무 성능 최적화의 기본(지연 로딩)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "user_id", nullable = false)
    private Long userId; //유저 서비스가 분리되어 있다고 가정하고 id 만 저장

    @Column(nullable = false)
    private LocalDateTime issuedAt; //언제 발급받았는지

    @Column(nullable = false)
    private boolean isUsed = false; //쿠폰 사용 여부

    /**
     * service계층에서 new CouponIssue(coupon, userId)를 호출할 때 사용됨
     * */
    public CouponIssue(Coupon coupon, Long userId){
        this.coupon = coupon;
        this.userId = userId;
        this.issuedAt = LocalDateTime.now();
        this.isUsed = false;
    }

}
