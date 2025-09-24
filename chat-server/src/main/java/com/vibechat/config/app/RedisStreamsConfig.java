package com.vibechat.config.app;

import com.vibechat.dto.StreamMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Redis Streams 기반 메시지 큐 설정
 *
 * 아키텍처:
 * - Room별 Stream: room:{roomId}:messages
 * - Broadcast 방식: XREAD로 실시간 읽기
 * - DLQ 처리: Consumer Group으로 오프라인 유저 처리
 */
@Configuration
@RequiredArgsConstructor
public class RedisStreamsConfig {

    private final RedisConnectionFactory connectionFactory;

    /**
     * Redis Streams 전용 RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisStreamsTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        // DTO 객체를 JSON으로 직렬화하기 위해 GenericJackson2JsonRedisSerializer를 사용합니다.
        template.setValueSerializer(new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Stream Message Listener Container
     * Redis Streams에서 메시지를 실시간으로 읽어오는 컨테이너
     */
    @Bean
    public StreamMessageListenerContainer<String, ObjectRecord<String, StreamMessageDto>> streamMessageListenerContainer() {
        var options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .batchSize(10)
                        .executor(streamExecutor())
                        .pollTimeout(Duration.ofSeconds(1))
                        .targetType(StreamMessageDto.class) // Set the target DTO class
                        .build();

        return StreamMessageListenerContainer.create(connectionFactory, options);
    }

    /**
     * Stream 처리용 Thread Pool
     */
    @Bean
    public Executor streamExecutor() {
        return Executors.newFixedThreadPool(4, r -> {
            Thread thread = new Thread(r, "redis-stream-processor");
            thread.setDaemon(true);
            return thread;
        });
    }
}