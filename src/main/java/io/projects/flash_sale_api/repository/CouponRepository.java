package io.projects.flash_sale_api.repository;

import io.projects.flash_sale_api.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 스프링 데이터의 JPA의 JpaRepository를 상속받아 인터페이스를 만들면 기본적인 CRUD는 자동으로 생성됨
 * save(entity) : 데이터 저장 (INSERT 또는 UPDATE)
 * findById(id) : PK로 데이터 단건 조회(SELECT)
 * findAll() : 전체 데이터 조회(SELECT * )
 * deleteById(id) : 데이터 삭제(DELETE)
 *
 * 메서드 이름만 규칙에 맞게 지어주면 스프링이 알아서 SQL(JPQL)을 짜줌
 *
 * DB테이블과 데이터를 주고받는 단순 파이프라인 구축에 쏟을 시간을 아껴
 * 오직 트래픽을 견디는 아키텍처와 비즈니스 로직에만 100% 집중할 수 있게 만들어 줌
 *
 */
public interface CouponRepository extends JpaRepository<Coupon, Long> {

}
