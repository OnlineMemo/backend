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

    private final ProxyManager<String> proxyManager;


    public ConsumptionProbe tryConsume(Long userId) {
        String rateLimitRequestKey = String.format("userId:%d:ratelimit_request", userId);
        Bucket bucket = proxyManager.builder().build(rateLimitRequestKey, () -> REQUEST_BUCKET_CONFIGURATION);
        return bucket.tryConsumeAndReturnRemaining(1);
    }
}
