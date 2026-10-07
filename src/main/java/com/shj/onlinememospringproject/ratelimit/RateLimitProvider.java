package com.shj.onlinememospringproject.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimitProvider {

    private static final long REQUEST_BUCKET_MAX_COUNT = 30;  // 최대 30회
    private static final long REQUEST_BUCKET_REFILL_TIME = 1000 * 10;  // 10초
    private static final BucketConfiguration REQUEST_BUCKET_CONFIGURATION = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(REQUEST_BUCKET_MAX_COUNT).refillIntervally(REQUEST_BUCKET_MAX_COUNT, Duration.ofMillis(REQUEST_BUCKET_REFILL_TIME)))  // 10초당 30회
            .build();
    private static final long EXHAUSTION_BUCKET_MAX_COUNT = 3;  // 최대 3회
    private static final long EXHAUSTION_BUCKET_REFILL_TIME = 1000 * 60 * 60;  // 60분 = 1시간
    private static final BucketConfiguration EXHAUSTION_BUCKET_CONFIGURATION = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(EXHAUSTION_BUCKET_MAX_COUNT).refillIntervally(EXHAUSTION_BUCKET_MAX_COUNT, Duration.ofMillis(EXHAUSTION_BUCKET_REFILL_TIME)))  // 1시간당 3회
            .build();

    private final ProxyManager<String> proxyManager;


    public ConsumptionProbe tryConsumeRequest(Long userId) {
        return getRequestBucket(userId).tryConsumeAndReturnRemaining(1);
    }

    public ConsumptionProbe tryConsumeExhaustion(Long userId) {
        return getExhaustionBucket(userId).tryConsumeAndReturnRemaining(1);
    }

    private Bucket getRequestBucket(Long userId) {
        String rateLimitRequestKey = String.format("userId:%d:ratelimit_request", userId);
        return proxyManager.builder().build(rateLimitRequestKey, () -> REQUEST_BUCKET_CONFIGURATION);
    }

    private Bucket getExhaustionBucket(Long userId) {
        String rateLimitExhaustionKey = String.format("userId:%d:ratelimit_exhaustion", userId);
        return proxyManager.builder().build(rateLimitExhaustionKey, () -> EXHAUSTION_BUCKET_CONFIGURATION);
    }
}
