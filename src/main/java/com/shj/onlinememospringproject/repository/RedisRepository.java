package com.shj.onlinememospringproject.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.StringTokenizer;

@Repository
@RequiredArgsConstructor
public class RedisRepository {

    private final RedisTemplate<String, String> redisTemplate;


    public String getValue(String key) {  // key의 value 조회
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean refreshTTL(String key, long millisecond) {  // 만료 시간 갱신 (성공 여부를 Boolean으로 반환)
        return redisTemplate.expire(key, Duration.ofMillis(millisecond));
    }

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
        String value = getValue(key);
        if(value == null) return false;
        StringTokenizer typeStt = new StringTokenizer(value, ",");
        StringTokenizer fieldStt;

        while(typeStt.hasMoreTokens()) {
            fieldStt = new StringTokenizer(typeStt.nextToken(), ":");
            if(fieldStt.nextToken().equals("userId") == true) {
                if(fieldStt.nextToken().equals(userId.toString()) == true) {
                    return unlock(key);
                }
                else {
                    return false;  // 키는 존재하나, 본인의 락이 아님.
                }
            }
        }
        return false;
    }
}
