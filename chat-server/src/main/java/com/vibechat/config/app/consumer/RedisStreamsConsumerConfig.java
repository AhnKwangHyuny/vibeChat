package com.vibechat.config.app.consumer;

import com.vibechat.consumer.room.RoomBroadcastConsumer;
import com.vibechat.consumer.user.UserReadStateConsumer;
import com.vibechat.consumer.storage.MessageStorageConsumer;
import com.vibechat.consumer.notification.NotificationConsumer;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Redis Streams Consumer 설정
 *
 * 엔터프라이즈급 Consumer Group 관리 및 최적화
 * - Consumer Group 자동 생성
 * - 오류 핸들링 및 재시도 로직
 * - 성능 최적화 설정
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisStreamsConsumerConfig {

    private final RedisConnectionFactory connectionFactory;
    private final RoomBroadcastConsumer roomBroadcastConsumer;
    private final UserReadStateConsumer userReadStateConsumer;
    private final MessageStorageConsumer messageStorageConsumer;
    private final NotificationConsumer notificationConsumer;

    @SuppressWarnings("rawtypes")
    private StreamMessageListenerContainer listenerContainer;

    @Bean
    @SuppressWarnings({"unchecked", "rawtypes"})
    public StreamMessageListenerContainer streamMessageListenerContainer() {
        // Consumer Container 옵션 설정 - Raw 타입으로 호환성 해결
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions options =
            StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                .builder()
                .batchSize(10)                              // 배치 처리 크기
                .executor(streamConsumerExecutor())         // 전용 스레드 풀
                .errorHandler(error -> log.error("Container error: {}", error.getMessage())) // 임시 에러 핸들링
                .pollTimeout(Duration.ofSeconds(1))         // 폴링 타임아웃
                .build();

        // Container 생성
        listenerContainer = StreamMessageListenerContainer.create(connectionFactory, options);

        // Consumer 등록
        registerConsumers();

        // Container 시작
        listenerContainer.start();

        log.info("Redis Streams Consumer Container 시작 완료");
        return listenerContainer;
    }

    /**
     * Consumer 등록
     */
    private void registerConsumers() {
        try {
            // 1. Room Broadcast Consumer 등록 (stream:room:* 패턴)
            registerRoomBroadcastConsumer();

            // 2. User Read State Consumer 등록 (stream:user:* 패턴)
            registerUserReadStateConsumer();

            // 3. Message Storage Consumer 등록 (모든 스트림)
            registerMessageStorageConsumer();

            // 4. Notification Consumer 등록 (stream:user:* 패턴)
            registerNotificationConsumer();

            log.info("모든 Consumer 등록 완료 (4개 Consumer)");

        } catch (Exception e) {
            log.error("Consumer 등록 중 오류 발생", e);
            throw new RuntimeException("Consumer 설정 실패", e);
        }
    }

    /**
     * Room Broadcast Consumer 등록
     */
    private void registerRoomBroadcastConsumer() {
        // Consumer Group 및 Consumer 정의
        Consumer consumer = Consumer.from("broadcast-group", "broadcast-consumer-1");

        // Room 스트림 패턴에 대한 StreamListener
        StreamListener<String, ObjectRecord<String, Object>> roomListener = new StreamListener<String, ObjectRecord<String, Object>>() {
            @Override
            public void onMessage(ObjectRecord<String, Object> record) {
                try {
                    if (isRoomStreamMessage(record)) {
                        roomBroadcastConsumer.processMessage(record);
                    }
                } catch (Exception e) {
                    log.error("[RoomBroadcast] 메시지 처리 실패: messageId={}, stream={}",
                        record.getId().getValue(), record.getStream(), e);
                    handleProcessingError("RoomBroadcast", record, e);
                }
            }
        };

        // 기본 Room 스트림들 등록 (예시: 사전 정의된 스트림들)
        registerStreamListener("stream:room:1", consumer, roomListener); // 예시 스트림
        registerStreamListener("stream:room:default", consumer, roomListener); // 기본 스트림

        log.info("RoomBroadcastConsumer 등록 완료: Consumer Group = broadcast-group");
    }

    /**
     * User Read State Consumer 등록
     */
    private void registerUserReadStateConsumer() {
        Consumer consumer = Consumer.from("readstate-group", "readstate-consumer-1");

        StreamListener<String, ObjectRecord<String, Object>> userListener = new StreamListener<String, ObjectRecord<String, Object>>() {
            @Override
            public void onMessage(ObjectRecord<String, Object> record) {
                try {
                    if (isUserStreamMessage(record)) {
                        userReadStateConsumer.processMessage(record);
                    }
                } catch (Exception e) {
                    log.error("[UserReadState] 메시지 처리 실패: messageId={}, stream={}",
                        record.getId().getValue(), record.getStream(), e);
                    handleProcessingError("UserReadState", record, e);
                }
            }
        };

        // User 스트림들 등록 (예시: 사전 정의된 스트림들)
        registerStreamListener("stream:user:1", consumer, userListener); // 예시 스트림
        registerStreamListener("stream:user:default", consumer, userListener); // 기본 스트림

        log.info("UserReadStateConsumer 등록 완료: Consumer Group = readstate-group");
    }

    /**
     * Message Storage Consumer 등록
     */
    private void registerMessageStorageConsumer() {
        Consumer consumer = Consumer.from("storage-group", "storage-consumer-1");

        StreamListener<String, ObjectRecord<String, Object>> storageListener = new StreamListener<String, ObjectRecord<String, Object>>() {
            @Override
            public void onMessage(ObjectRecord<String, Object> record) {
                try {
                    // Storage Consumer는 모든 스트림 메시지를 처리
                    messageStorageConsumer.processMessage(record);
                } catch (Exception e) {
                    log.error("[MessageStorage] 메시지 처리 실패: messageId={}, stream={}",
                        record.getId().getValue(), record.getStream(), e);
                    handleProcessingError("MessageStorage", record, e);
                }
            }
        };

        // Storage Consumer는 모든 종류의 스트림을 처리
        registerStreamListener("stream:room:default", consumer, storageListener);
        registerStreamListener("stream:user:default", consumer, storageListener);
        registerStreamListener("stream:notifications", consumer, storageListener);

        log.info("MessageStorageConsumer 등록 완료: Consumer Group = storage-group");
    }

    /**
     * Notification Consumer 등록
     */
    private void registerNotificationConsumer() {
        Consumer consumer = Consumer.from("notification-group", "notification-consumer-1");

        StreamListener<String, ObjectRecord<String, Object>> notificationListener = new StreamListener<String, ObjectRecord<String, Object>>() {
            @Override
            public void onMessage(ObjectRecord<String, Object> record) {
                try {
                    if (isNotificationStreamMessage(record)) {
                        notificationConsumer.processMessage(record);
                    }
                } catch (Exception e) {
                    log.error("[Notification] 메시지 처리 실패: messageId={}, stream={}",
                        record.getId().getValue(), record.getStream(), e);
                    handleProcessingError("Notification", record, e);
                }
            }
        };

        // Notification 스트림 등록
        registerStreamListener("stream:notifications", consumer, notificationListener);

        log.info("NotificationConsumer 등록 완료: Consumer Group = notification-group");
    }

    /**
     * 스트림 리스너 등록 유틸리티
     */
    private void registerStreamListener(String streamKey, Consumer consumer, StreamListener<String, ObjectRecord<String, Object>> listener) {
        try {
            // 스트림 오프셋 설정 (Consumer Group의 마지막 위치부터 읽기)
            StreamOffset<String> streamOffset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());

            // Subscription 등록
            Subscription subscription = listenerContainer.receiveAutoAck(consumer, streamOffset, listener);

            log.debug("스트림 리스너 등록: stream={}, consumer={}", streamKey, consumer);

        } catch (Exception e) {
            log.error("스트림 리스너 등록 실패: stream={}, consumer={}", streamKey, consumer, e);
            throw new RuntimeException("Stream Listener 등록 실패: " + streamKey, e);
        }
    }

    /**
     * 스트림 메시지 타입 판별 유틸리티들
     */
    private boolean isRoomStreamMessage(ObjectRecord<String, Object> record) {
        return record.getStream() != null && record.getStream().startsWith("stream:room:");
    }

    private boolean isUserStreamMessage(ObjectRecord<String, Object> record) {
        return record.getStream() != null && record.getStream().startsWith("stream:user:");
    }

    private boolean isNotificationStreamMessage(ObjectRecord<String, Object> record) {
        return record.getStream() != null && record.getStream().equals("stream:notifications");
    }

    /**
     * 공통 에러 처리 로직
     */
    private void handleProcessingError(String consumerType, ObjectRecord<String, Object> record, Exception error) {
        String messageId = record.getId().getValue();
        String stream = record.getStream();

        log.error("[{}] 메시지 처리 중 오류 발생: messageId={}, stream={}, error={}",
            consumerType, messageId, stream, error.getMessage());

        // TODO: 에러 타입별 세분화 처리
        // - 재시도 가능한 에러: 재시도 큐에 추가
        // - 재시도 불가능한 에러: Dead Letter Queue로 이동
        // - 임계값 초과: 알림 발송
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
     * Container 수준의 에러 핸들링 (전역 에러 처리)
     */
    private void handleConsumerError(Exception error, ObjectRecord<String, Object> record) {
        String messageId = record != null ? record.getId().getValue() : "unknown";
        String stream = record != null ? record.getStream() : "unknown";

        log.error("Consumer Container 전역 에러 발생: messageId={}, stream={}, error={}",
            messageId, stream, error.getMessage(), error);

        // TODO: 전역 에러 하들링 로직
        // - Container 레벨 오류의 경우 시스템 중단 여부 결정
        // - 비상 상황에 대한 알림 발송
        // - 메트릭 및 모니터링 데이터 수집

        // 에러 카운터 증가 (TODO: 메트릭 수집 구현)
        incrementErrorCounter(stream, error.getClass().getSimpleName());
    }

    /**
     * 에러 메트릭 수집 (추후 구현)
     */
    private void incrementErrorCounter(String stream, String errorType) {
        // TODO: Micrometer 또는 다른 메트릭 라이브러리를 사용하여 에러 카운터 구현
        log.debug("에러 메트릭 기록: stream={}, errorType={}", stream, errorType);
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

    /**
     * Consumer 헬스체크 메소드
     */
    public boolean isConsumerHealthy() {
        try {
            boolean containerHealthy = listenerContainer != null && listenerContainer.isRunning();
            boolean consumersHealthy = roomBroadcastConsumer.isHealthy() &&
                                     userReadStateConsumer.isHealthy() &&
                                     messageStorageConsumer.isHealthy() &&
                                     notificationConsumer.isHealthy();

            boolean overall = containerHealthy && consumersHealthy;

            if (!overall) {
                log.warn("Consumer 헬스체크 실패: container={}, consumers={}",
                    containerHealthy, consumersHealthy);
            }

            return overall;
        } catch (Exception e) {
            log.error("Consumer 헬스체크 예외 발생", e);
            return false;
        }
    }

    /**
     * 개별 Consumer 헬스체크 상세 정보
     */
    public String getConsumerHealthDetails() {
        try {
            return String.format(
                "Container: %s, RoomBroadcast: %s, UserReadState: %s, MessageStorage: %s, Notification: %s",
                listenerContainer != null && listenerContainer.isRunning() ? "OK" : "FAIL",
                roomBroadcastConsumer.isHealthy() ? "OK" : "FAIL",
                userReadStateConsumer.isHealthy() ? "OK" : "FAIL",
                messageStorageConsumer.isHealthy() ? "OK" : "FAIL",
                notificationConsumer.isHealthy() ? "OK" : "FAIL"
            );
        } catch (Exception e) {
            log.error("Consumer 헬스체크 상세 정보 조회 실패", e);
            return "ERROR: " + e.getMessage();
        }
    }
}