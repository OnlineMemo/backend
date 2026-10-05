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

    private static final long BUCKET_MAX_COUNT = 30;  // 최대 30회
    private static final long BUCKET_REFILL_TIME = 1000 * 10;  // 10초
    private static final BucketConfiguration BUCKET_CONFIGURATION = BucketConfiguration.builder()
            .addLimit(limit -> limit.capacity(BUCKET_MAX_COUNT).refillIntervally(BUCKET_MAX_COUNT, Duration.ofMillis(BUCKET_REFILL_TIME)))  // 10초당 30회
            .build();

    private final ProxyManager<String> proxyManager;


    public ConsumptionProbe tryConsume(Long userId) {
        String rateLimitKey = String.format("userId:%d:rate_limit", userId);
        Bucket bucket = proxyManager.builder().build(rateLimitKey, () -> BUCKET_CONFIGURATION);
        return bucket.tryConsumeAndReturnRemaining(1);
    }
}
