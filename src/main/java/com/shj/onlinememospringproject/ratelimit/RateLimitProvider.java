package com.shj.onlinememospringproject.ratelimit;

import com.shj.onlinememospringproject.repository.CaffeineRepository;
import com.shj.onlinememospringproject.repository.RedisRepository;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

// - [1차] 요청 제한 : 10초당 30회 초과 시 429 예외 응답 (메모리 저장)
// - [2차] 24시간 차단 : 1시간 내 버킷 소진 3회 시 (메모리 저장, 차단 횟수는 Redis 저장)
// - [3차] 영구정지 : 24시간 차단 3회 누적 시 (MySQL user.user_state=BLOCKED 저장)
@Component
@RequiredArgsConstructor
public class RateLimitProvider {

    private static final long REQUEST_BUCKET_MAX_COUNT = 30;  // 요청제한 최대 30회
    private static final long REQUEST_BUCKET_REFILL_TIME = 1000 * 10;  // 10초
    private static final BucketConfiguration REQUEST_BUCKET_CONFIGURATION = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(REQUEST_BUCKET_MAX_COUNT).refillIntervally(REQUEST_BUCKET_MAX_COUNT, Duration.ofMillis(REQUEST_BUCKET_REFILL_TIME)))  // 10초당 30회
            .build();
    private static final long EXHAUSTION_BUCKET_MAX_COUNT = 3;  // 버킷소진 최대 3회
    private static final long EXHAUSTION_BUCKET_REFILL_TIME = 1000 * 60 * 60;  // 60분 = 1시간
    private static final BucketConfiguration EXHAUSTION_BUCKET_CONFIGURATION = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(EXHAUSTION_BUCKET_MAX_COUNT).refillIntervally(EXHAUSTION_BUCKET_MAX_COUNT, Duration.ofMillis(EXHAUSTION_BUCKET_REFILL_TIME)))  // 1시간당 3회
            .build();
    private static final long BLOCK_MAX_COUNT = 3;  // 24시간 차단 최대 3회
    private static final long BLOCK_EXPIRE_TIME = 1000 * 60 * 60 * 24;  // 1440분 = 24시간

    private final ProxyManager<String> proxyManager;
    private final BlockedUserProvider blockedUserProvider;
    private final CaffeineRepository caffeineRepository;
    private final RedisRepository redisRepository;

    @Value("${rate-limit.storage:caffeine}")
    private String rateLimitStorage;


    public ConsumptionProbe tryConsumeRequest(Long userId) {
        String rateLimitRequestKey = String.format("userId:%d:ratelimit_request", userId);
        Bucket bucket = proxyManager.builder().build(rateLimitRequestKey, () -> REQUEST_BUCKET_CONFIGURATION);
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    public ConsumptionProbe tryConsumeExhaustion(Long userId) {
        String rateLimitExhaustionKey = String.format("userId:%d:ratelimit_exhaustion", userId);
        Bucket bucket = proxyManager.builder().build(rateLimitExhaustionKey, () -> EXHAUSTION_BUCKET_CONFIGURATION);
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    public void blockUser(Long userId) {  // 24시간 차단
        String rateLimitBlockKey = String.format("userId:%d:ratelimit_block", userId);
        String unblockTime = String.valueOf(System.currentTimeMillis() + BLOCK_EXPIRE_TIME);  // 차단해제 예정 시각
        if(rateLimitStorage.equals("redis")) redisRepository.setValue(rateLimitBlockKey, unblockTime, BLOCK_EXPIRE_TIME);
        else caffeineRepository.setValue(rateLimitBlockKey, unblockTime, BLOCK_EXPIRE_TIME);

        String rateLimitBlockCountKey = String.format("userId:%d:ratelimit_block_count", userId);
        Long blockCount = redisRepository.updateCount(rateLimitBlockCountKey, 1);  // 차단 횟수 누적 (Redis 저장, TTL 없음)
        if(blockCount >= BLOCK_MAX_COUNT) {  // 24시간 차단 3회 누적 시 영구정지
            blockedUserProvider.banUser(userId);
            redisRepository.unlock(rateLimitBlockCountKey);  // 영구정지 이후에는 불필요하므로 24시간 차단 횟수 삭제
        }
    }

    public long getRemainBlockTime(Long userId) {  // 24시간 차단이 앞으로 얼마나 남았는지(밀리초)
        String rateLimitBlockKey = String.format("userId:%d:ratelimit_block", userId);
        String unblockTime = rateLimitStorage.equals("redis")
                ? redisRepository.getValue(rateLimitBlockKey)
                : caffeineRepository.getValue(rateLimitBlockKey);
        if(unblockTime == null) return 0;
        return Math.max(Long.parseLong(unblockTime) - System.currentTimeMillis(), 0);
    }

    public void deleteBlockCount(Long userId) {  // 24시간 차단 횟수 삭제
        String rateLimitBlockCountKey = String.format("userId:%d:ratelimit_block_count", userId);
        redisRepository.unlock(rateLimitBlockCountKey);
    }
}
