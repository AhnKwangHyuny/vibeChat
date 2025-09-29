package com.vibechat.config.properties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import jakarta.annotation.PostConstruct;

@Configuration
@Slf4j
public class RedisConfig {

    @PostConstruct
    public void init() {
        log.info("RedisConfig 초기화 시작");
    }

    /**
     * 일반적인 Redis 작업용 템플릿 (JSON 직렬화)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("RedisTemplate Bean 생성 중...");

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key는 String으로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value는 JSON으로 직렬화 (Object 타입 지원)
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        log.info("RedisTemplate Bean 생성 완료");
        return template;
    }

    /**
     * String 전용 Redis 템플릿 (Presence 서비스용)
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("StringRedisTemplate Bean 생성 중...");

        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);

        log.info("StringRedisTemplate Bean 생성 완료");
        return template;
    }

    /**
     * Redis 연결 상태 확인
     */
    @PostConstruct
    public void checkRedisConnection() {
        log.info("Redis 연결 상태 확인 중...");
        // 실제 연결은 첫 번째 작업 시에 이루어짐
    }
}
