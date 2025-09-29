package com.shj.onlinememospringproject.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.StringTokenizer;

@Repository
@RequiredArgsConstructor
public class RedisRepository {  // Redis DB

    private final RedisTemplate<String, String> redisTemplate;


    public String getValue(String key) {  // key의 value 조회
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean refreshTTL(String key, long millisecond) {  // 만료 시간 갱신 (성공 여부를 Boolean으로 반환)
        return redisTemplate.expire(key, Duration.ofMillis(millisecond));
    }

    public Boolean checkOwner(String key, Long userId) {  // 본인의 락이 맞는지 검증
        // true : 키가 존재하고, 본인의 락이 맞음.
        // false : 키는 존재하나, 본인의 락이 아님.
        // null : 키가 존재하지 않음.
        String value = getValue(key);
        if(value == null) return null;
        StringTokenizer typeStt = new StringTokenizer(value, ",");
        StringTokenizer fieldStt;

        while(typeStt.hasMoreTokens()) {
            fieldStt = new StringTokenizer(typeStt.nextToken(), ":");
            if(fieldStt.nextToken().equals("userId") == true) {
                if(fieldStt.nextToken().equals(userId.toString()) == true) {
                    return true;
                }
                else {
                    return false;  // 키는 존재하나, 본인의 락이 아님.
                }
            }
        }
        return false;
    }

    // - setValue() : 키의 존재여부와 관계없이 덮어씌워서라도 저장하며, 반환값 없음.
    public void setValue(String key, String value, Long millisecond) {  // millisecond = null 허용
        if(millisecond != null) {  // value와 TTL 모두 지정해서 저장
            redisTemplate.opsForValue().set(key, value, Duration.ofMillis(millisecond));
        }
        else {  // value만 지정해서 저장하며, TTL은 만료없음
            redisTemplate.opsForValue().set(key, value);
        }
    }

    // - lock() : 키가 존재하지 않는 경우에만 저장하며, Boolean 반환.
    public Boolean lock(String key, String value, long millisecond) {  // 락 획득 & 생성 (성공 여부를 Boolean으로 반환)
        // true : 현재 키가 Redis에 없어서 성공적으로 락을 생성하고, 획득함.
        // false : 이미 해당 키가 Redis에 존재하여 락 획득 실패 (다른 누군가가 이미 락을 잡고 있음)
        return redisTemplate
                .opsForValue()
                .setIfAbsent(key, value, Duration.ofMillis(millisecond));  // 'SET {key} {value} NX PX {millisecond}' 명령어
    }

    public Boolean unlock(String key) {  // 락 해제 (본인 무관)
        return redisTemplate.delete(key);  // 'DEL {key}' 명령어
    }

    public Boolean unlockOwner(String key, Long userId) {  // 본인의 락 해제
        // true : 본인의 락 해제 성공.
        // false : 본인의 락 해제 실패.
        // null : 키가 존재하지 않음.
        // ==> 즉, 본인의 락이 존재할때만 해제함.
        Boolean isOwnLock = checkOwner(key, userId);
        if(isOwnLock == null) return null;
        else {
            if(isOwnLock == true) return unlock(key);
            else return false;
        }
    }

    public Long updateCount(String key, long count) {
        // 숫자 반환 (키 존재 O) : 기존값에 +count 또는 -count 후 결과값 반환. (TTL 유지)
        // 숫자 반환 (키 존재 X) : 값을 1 또는 -1로 신규 저장 후 해당값 반환. (단, TTL 무한)
        // null 반환 : count 파라미터 값을 0으로 호출한 경우
        // throw Exception : 기존값이 정수 자료형이 아닌 경우
        if(count > 0) {
            return redisTemplate.opsForValue().increment(key, count);
        }
        else if(count < 0) {
            return redisTemplate.opsForValue().decrement(key, Math.abs(count));
        }
        return null;  // else if(count == 0)
    }

//    public void updateValue(String key, String value, Long millisecond) {  // millisecond = null 허용
//        Long currentTTL = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
//        if(currentTTL == null || currentTTL == -2) return;  // 키가 존재하지 않는 경우
//
//        // (millisecond != null)이 true면 value와 TTL 모두 업데이트, false면 value만 업데이트하고 TTL은 그대로 유지.
//        Long nextTTL = (millisecond != null) ? millisecond : currentTTL;
//        redisTemplate.opsForValue().set(key, value, Duration.ofMillis(nextTTL));
//    }
}
