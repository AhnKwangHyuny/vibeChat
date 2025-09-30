package com.vibechat.config.app;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Redis Streams 기본 설정
 *
 * 필수 Bean들만 제공:
 * - StreamMessageListenerContainer
 * - Consumer 전용 Executor
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisStreamsConsumerConfig {

    private final RedisConnectionFactory connectionFactory;

    @SuppressWarnings("rawtypes")
    private StreamMessageListenerContainer listenerContainer;

    @Bean
    @SuppressWarnings({"unchecked", "rawtypes"})
    public StreamMessageListenerContainer streamMessageListenerContainer() {
        // Consumer Container 옵션 설정
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions options =
            StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .builder()
                .batchSize(10)
                .executor(streamConsumerExecutor())
                .errorHandler(error -> log.error("Redis Streams Container error: {}", error.getMessage()))
                .pollTimeout(Duration.ofSeconds(1))
                .build();

        // Container 생성
        listenerContainer = StreamMessageListenerContainer.create(connectionFactory, options);

        // Container 시작
        listenerContainer.start();

        log.info("Redis Streams Consumer Container 생성 완료 (Consumer 등록은 ConsumerRegistrationService에서 처리)");
        return listenerContainer;
    }


    /**
     * Consumer 전용 스레드 풀
     */
    @Bean("streamConsumerExecutor")
    public Executor streamConsumerExecutor() {
        return Executors.newFixedThreadPool(10, r -> {
            Thread thread = new Thread(r);
            thread.setName("redis-streams-consumer-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        });
    }


    /**
     * 애플리케이션 종료 시 Consumer Container 정리
     */
    @PreDestroy
    public void cleanup() {
        if (listenerContainer != null) {
            log.info("Redis Streams Consumer Container 종료 시작...");
            listenerContainer.stop();
            log.info("Redis Streams Consumer Container 종료 완료");
        }
    }

}