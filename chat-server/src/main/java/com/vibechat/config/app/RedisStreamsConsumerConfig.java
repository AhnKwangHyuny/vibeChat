package com.vibechat.config.app;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Redis Streams Consumer 설정
 *
 * Lazy Start Pattern 구현:
 * - Container는 생성만 하고 시작하지 않음
 * - ConsumerRegistrationService가 첫 Listener 등록 시 Container 시작
 * - Listener가 없는 Container의 불필요한 폴링 방지
 *
 * 제공하는 Bean
 * - StreamMessageListenerContainer: Listener 관리 컨테이너 (시작 안 함)
 * - streamConsumerExecutor: Consumer 전용 스레드 풀
 */
@Configuration
@Slf4j
public class RedisStreamsConsumerConfig {

    private final RedisConnectionFactory connectionFactory;

    public RedisStreamsConsumerConfig(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        log.info("[RedisStreamsConfig] ConnectionFactory 주입 완료");
    }

    @SuppressWarnings("rawtypes")
    private StreamMessageListenerContainer listenerContainer;

    @PostConstruct
    public void validateConnectionFactory() {
        try {
            RedisConnection connection = connectionFactory.getConnection();
            String pingResult = connection.ping();
            connection.close();

            log.info("[RedisStreamsConfig] ConnectionFactory 연결 성공: {}", pingResult);

        } catch (Exception e) {
            log.error("[RedisStreamsConfig] FATAL: ConnectionFactory 연결 실패!", e);
            throw new IllegalStateException("Redis 연결 실패 - Redis 서버 실행 상태를 확인하세요", e);
        }
    }

    /**
     * StreamMessageListenerContainer Bean 생성
     *
     * 중요: start()를 호출하지 않음 (Lazy Start Pattern)
     * - ConsumerRegistrationService가 첫 Listener 등록 시 start() 호출
     * - Listener 없이 시작하면 메시지 폴링이 작동하지 않을 수 있음
     */
    @Bean
    @SuppressWarnings({"unchecked", "rawtypes"})
    public StreamMessageListenerContainer streamMessageListenerContainer() {
        
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions options =
            StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .builder()
                .batchSize(10)
                .executor(streamConsumerExecutor())
                .errorHandler(error -> {
                    log.error("[RedisStreamsContainer] 오류 발생: {}", error.getMessage());
                    
                    if (error.getMessage().contains("Unable to connect")) {
                        log.warn("[RedisStreamsContainer] Redis 재연결 필요 - Container 재시작 고려");
                    }
                })
                .pollTimeout(Duration.ofSeconds(5))
                .build();

        listenerContainer = StreamMessageListenerContainer.create(connectionFactory, options);

        log.info("[RedisStreamsConfig] Container 생성 완료 (시작 안 함 - Lazy Start Pattern)");
        return listenerContainer;
    }


    /**
     * Consumer 전용 스레드 풀
     *
     * 고정 크기 10개 스레드로 모든 Consumer 처리
     */
    @Bean("streamConsumerExecutor")
    public Executor streamConsumerExecutor() {
        return Executors.newFixedThreadPool(10, r -> {
            Thread thread = new Thread(r);
            thread.setName("redis-streams-consumer-" + thread.threadId());
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 애플리케이션 종료 시 Container 정리
     */
    @PreDestroy
    public void cleanup() {
        if (listenerContainer != null && listenerContainer.isRunning()) {
            log.info("[RedisStreamsConfig] Container 종료 시작...");
            listenerContainer.stop();
            log.info("[RedisStreamsConfig] Container 종료 완료");
        }
    }
}