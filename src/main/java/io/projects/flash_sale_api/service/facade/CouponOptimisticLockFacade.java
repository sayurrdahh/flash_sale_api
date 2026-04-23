package io.projects.flash_sale_api.service.facade;

import io.projects.flash_sale_api.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * 범용적인 데이터 정합성 보장 중요
 *
 * 추후 대규모 트래픽 처리를 위해 Redis-RDB 이중 저장 구조를 도입하기 전 RDB 자체 기능만으로 동시성 방어
 *
 * 낙관적 락(Optimistic Lock) : 백엔드 서버에서 예외를 잡아서 성공할 때까지
 * (재고가 소진될 때까지) 자동으로 재시도(Retry) 하는 로직을 짜줌
 * 트랜잭션을 분리하는 Facade(퍼사드) 패턴 사용
 *
 * Facade 클래스는 건출물의 정면이라는 뜻으로, 복잡한 내부 로직이나 여러 서비스를 하나로 묶어 클라이언트(Controller)가 사용하기 쉽게 만들어주는 '창구' 역할
 * 이번 프로젝트에서 Facade를 설명하는 가장 큰 이유는 트랜잭션의 범위 조절 때문
 *
 * 기존에는 Controller 가 직접 Service를 호출했다면, 이제는 Facade를 호출함.
 *
 * 왜 Facade에서 재시도 하나요?
 * 1) @Transactional 의 한계 : 스프링의 트랜잭션 내에서 낙관적 락 예외가 발생하면, 해당 트랜잭션은 이미 롤백(rollback)상태로 확정됨
 * 2) 문제 상황 : 만약 service 내부의 while 문 안에서 재시도를 하려고 하면,
 * 이미 망가진(롤백 확정된) 트랜잭션을 계속 재사용하게 되어 무조건 실패하게 된다.
 * 3) 해결책(Facade) : Facade는 @Transactional 이 없다. Facade가 while문을 돌면서 매번 새로운 트랜잭셔을 가진
 * Service 메서드를 호출하기 때문에, 실패하더라도 다음 시도에서는 새로운 마음으로 깨끗한 트랜잭션을 시작할 수 있다.
 *
 * Facade 클래스의 주요 역할 정리
 * 1. 로직의 조합 : 다른 서비스 로직이 추가되면 facade에서 여러 서비스를 순서대로 호출하도록 구성하여 코드의 응집도를 높임
 *
 * 낙관적 락 도입 시 @Transactional 의 프록시 특성상 서비스 내부 재시도가 불가능한 문제를 해결하기 위해
 * Facade 패턴을 적용하여 트랜잭션 경계를 분리하고 안정적인 재시도 로직을 구현했습니다.
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class CouponOptimisticLockFacade {

    private final CouponService couponService;

    public void issue(Long couponId, Long userId) throws InterruptedException{
        //성공할 때까지 무한 반복 또는 최대 재시도 횟수까지 반복
        while(true){
            try{
                //실제 서비스 로직 호출
                couponService.issue(couponId, userId);
                break;
            }catch (ObjectOptimisticLockingFailureException e){
                //충돌 발생 (다른 스레드가 먼저 버전 올림)
                log.info("버전 충돌 발생. 50ms 대기 후 재시도합니다. userId:{}", userId);
                //DB 부하를 줄이기 위해 아주 잠깐 대기 후 다시 시도
                Thread.sleep(50);
            }catch (Exception e){
                //수량 소진 등 미즈니스 로직 예외는 그대로 밖으로 던짐
                throw e;
            }
        }

    }

}
