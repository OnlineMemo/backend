package com.shj.onlinememospringproject.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Repository;

@Repository
public class CaffeineRepository {  // Caffeine 서버 메모리

    private final Cache<String, CaffeineValue> caffeineCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .build();


    public String getValue(String key) {  // key의 value 조회
        CaffeineValue caffeineValue = caffeineCache.getIfPresent(key);
        if(caffeineValue == null) return null;

        if(caffeineValue.expiredTime() != null && caffeineValue.expiredTime() <= System.currentTimeMillis()) {  // TTL이 지난 경우
            caffeineCache.invalidate(key);
            return null;
        }
        return caffeineValue.value();
    }

    // - setValue() : 키의 존재여부와 관계없이 덮어씌워서라도 저장하며, 반환값 없음.
    public void setValue(String key, String value, Long millisecond) {  // millisecond = null 허용
        Long expiredTime = (millisecond != null) ? (System.currentTimeMillis() + millisecond) : null;  // null이면 만료없음
        caffeineCache.put(key, new CaffeineValue(value, expiredTime));
    }


    private record CaffeineValue(String value, Long expiredTime) { }  // value와 만료 시각을 함께 저장
}
