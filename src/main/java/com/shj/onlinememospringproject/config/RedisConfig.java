package com.shj.onlinememospringproject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String redisHost;
    @Value("${spring.redis.port}")
    private int redisPort;
    @Value("${spring.redis.password:}")  // Local 환경에서는 없어도 되므로, 기본값으로 빈 문자열 할당.
    private String redisPw;
    @Value("${server.env}")
    private String serverEnv;


    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();  // Redis 서버의 주소 정보 (host, port, password 등)
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);

        LettuceClientConfiguration clientConfig;  // Redis 클라이언트의 접속 방식 (timeout, SSL 등)
        if(serverEnv.equals("prod")) {
            if(!redisPw.isEmpty()) redisConfig.setPassword(RedisPassword.of(redisPw));

            clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofSeconds(5))
                    .useSsl()
                    .build();
        }
        else {
            clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(Duration.ofSeconds(5))
                    .build();
        }

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }
}
