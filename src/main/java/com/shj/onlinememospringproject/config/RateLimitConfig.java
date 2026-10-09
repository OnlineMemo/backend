package com.shj.onlinememospringproject.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.caffeine.Bucket4jCaffeine;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    private static final ExpirationAfterWriteStrategy EXPIRATION_STRATEGY =
            ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10));  // 버킷이 가득 차고 10초 뒤 삭제


    @Bean
    @ConditionalOnProperty(name = "rate-limit.storage", havingValue = "caffeine", matchIfMissing = true)
    public ProxyManager<String> caffeineProxyManager() {  // 서버 메모리 저장 (단일 인스턴스용)
        return Bucket4jCaffeine.<String>builderFor(Caffeine.newBuilder().maximumSize(10000))
                .expirationAfterWrite(EXPIRATION_STRATEGY)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "rate-limit.storage", havingValue = "redis")
    public ProxyManager<String> redisProxyManager(LettuceConnectionFactory redisConnectionFactory) {  // Redis 저장 (다중 인스턴스용)
        RedisClient redisClient = (RedisClient) redisConnectionFactory.getRequiredNativeClient();  // 기존 RedisConfig의 클라이언트 재사용
        StatefulRedisConnection<String, byte[]> connection = redisClient
                .connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        return Bucket4jLettuce.casBasedBuilder(connection)
                .expirationAfterWrite(EXPIRATION_STRATEGY)
                .build();
    }
}
