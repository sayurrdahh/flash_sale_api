package io.projects.flash_sale_api.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository(인터페이스)
 * - 스프링이 제공하는 자동화 마법
 * - 개발자가 인터페이스만 만들어 두면 스프링 프로젝트를 실행할 때
 * 내부적으로 그 인터페이스를 구현하는 가짜 객체(프록시)를 만들어서
 * insert, select 같은 쿼리를 알아서 짜줌.
 *
 * 반면 이 repository는 StringRedisTemplate이라는 도구를 사용해 직접 코드를 제어해야 함
 * 레디스의 원자적 연산(INCR, SADD)을 활용한 초고속 동시성 제어를 해야하는데
 * 자동화된 인터페이스로는 이런 세밀한 명령어를 직접 컨트롤하기 어렵기 때문에
 * 대규모 트래픽을 다루는 실무 환경에서는 StringRedisTemplate을 주입받아 클래스로 직접 구현하는 방식을 선호함
 * */

@Repository
@RequiredArgsConstructor
public class CouponRedisRepository {

    private final StringRedisTemplate redisTemplate;

    //1.Set을 이용한 중복 발급 체크(Redis 'SADD' 명령어_
    //0(1)의 속도로 데이터가 없으면 추가 후 true 반환, 이미 있으면 false 반환
    public Boolean addIfAbsent(Long couponId, Long userId){
        String key = "coupon:" + couponId + ":users";
        return redisTemplate.opsForSet().add(key, userId.toString()) == 1L;
    }

    /**
     * opsForSet() : 중복 없는 집합 다루기
     * redisTemplate.opsForSet().add(key, userId.toString()) == 1L;
     * 지정된 key에 userId를 넣었을 때
     * 레디스의 add 메서드는 새롭게 추가된 데이터 개수를 Long 타입의 숫자로 반환함
     * 1이 반환되면 성공적으로 1개 추가됨.
     * 0이 반환되면 이미 있는 유저 (중복이라 무시, 추가 안됨)
     * */

    //2. 원자적 수량 증가(Redis 'INCR')명령어
    //동시성 충돌 없이 무조건 1씩 증가시킨 후의 현재 숫자를 반호나
    public Long increment(Long couponId){
        String key = "coupon:" + couponId + ":count";
        return redisTemplate.opsForValue().increment(key);
    }
}
