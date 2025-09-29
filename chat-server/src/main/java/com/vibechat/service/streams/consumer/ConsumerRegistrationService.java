package com.vibechat.service.streams.consumer;

import com.vibechat.consumer.core.MessageConsumer;
import com.vibechat.consumer.room.RoomBroadcastConsumer;
import com.vibechat.consumer.user.UserReadStateConsumer;
import com.vibechat.consumer.storage.MessageStorageConsumer;
import com.vibechat.consumer.notification.NotificationConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Service;

/**
 * Consumer Group 동적 등록 관리 서비스
 *
 * 새로운 스트림을 기존 Consumer Group에 동적으로 추가하거나 제거하는
 * 엔터프라이즈급 Consumer Group 관리 시스템
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsumerRegistrationService {

    private final StreamMessageListenerContainer<String, ObjectRecord<String, Object>> listenerContainer;
    private final RoomBroadcastConsumer roomBroadcastConsumer;
    private final UserReadStateConsumer userReadStateConsumer;
    private final MessageStorageConsumer messageStorageConsumer;
    private final NotificationConsumer notificationConsumer;

    /**
     * 방 스트림을 모든 Consumer Group에 추가
     */
    public void addRoomStreamToConsumerGroups(String streamKey) {
        try {
            log.info("[ConsumerRegistration] 방 스트림 Consumer Group 등록 시작: streamKey={}", streamKey);

            // 1. RoomBroadcastConsumer에 방 스트림 추가
            registerStreamForConsumer(streamKey, "broadcast-group", "broadcast-consumer-1", roomBroadcastConsumer);

            // 2. MessageStorageConsumer에 방 스트림 추가 (모든 스트림 처리)
            registerStreamForConsumer(streamKey, "storage-group", "storage-consumer-1", messageStorageConsumer);

            // UserReadStateConsumer와 NotificationConsumer는 User 스트림만 처리하므로 제외

            log.info("[ConsumerRegistration] 방 스트림 Consumer Group 등록 완료: streamKey={}", streamKey);

        } catch (Exception e) {
            log.error("[ConsumerRegistration] 방 스트림 Consumer Group 등록 실패: streamKey={}", streamKey, e);
            throw new ConsumerRegistrationException("방 스트림 Consumer Group 등록에 실패했습니다", e);
        }
    }

    /**
     * 사용자 스트림을 관련 Consumer Group에 추가
     */
    public void addUserStreamToConsumerGroups(String streamKey) {
        try {
            log.info("[ConsumerRegistration] 사용자 스트림 Consumer Group 등록 시작: streamKey={}", streamKey);

            // 1. UserReadStateConsumer에 사용자 스트림 추가
            registerStreamForConsumer(streamKey, "readstate-group", "readstate-consumer-1", userReadStateConsumer);

            // 2. MessageStorageConsumer에 사용자 스트림 추가 (모든 스트림 처리)
            registerStreamForConsumer(streamKey, "storage-group", "storage-consumer-1", messageStorageConsumer);

            // 3. NotificationConsumer에 사용자 스트림 추가
            registerStreamForConsumer(streamKey, "notification-group", "notification-consumer-1", notificationConsumer);

            log.info("[ConsumerRegistration] 사용자 스트림 Consumer Group 등록 완료: streamKey={}", streamKey);

        } catch (Exception e) {
            log.error("[ConsumerRegistration] 사용자 스트림 Consumer Group 등록 실패: streamKey={}", streamKey, e);
            throw new ConsumerRegistrationException("사용자 스트림 Consumer Group 등록에 실패했습니다", e);
        }
    }

    /**
     * 특정 스트림을 Consumer에 등록하는 유틸리티 메소드
     */
    private void registerStreamForConsumer(String streamKey, String consumerGroup, String consumerId,
                                         MessageConsumer consumer) {
        try {
            // Redis Consumer 정의
            Consumer redisConsumer = Consumer.from(consumerGroup, consumerId);

            // StreamListener 생성 - Consumer의 processMessage 메소드 위임
            StreamListener<String, ObjectRecord<String, Object>> listener =
                new StreamListener<String, ObjectRecord<String, Object>>() {
                    @Override
                    public void onMessage(ObjectRecord<String, Object> record) {
                        try {
                            // 스트림 타입 필터링
                            if (shouldProcessStream(streamKey, consumer)) {
                                consumer.processMessage(record);
                            }
                        } catch (Exception e) {
                            log.error("[ConsumerRegistration] 메시지 처리 실패: consumer={}, streamKey={}, messageId={}",
                                consumer.getClass().getSimpleName(), streamKey, record.getId().getValue(), e);
                        }
                    }
                };

            // Stream Offset 설정 (Consumer Group의 마지막 위치부터 읽기)
            StreamOffset<String> streamOffset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());

            // Container에 Subscription 등록
            listenerContainer.receiveAutoAck(redisConsumer, streamOffset, listener);

            log.debug("[ConsumerRegistration] 스트림 리스너 등록 완료: stream={}, consumer={}, group={}",
                streamKey, consumer.getClass().getSimpleName(), consumerGroup);

        } catch (Exception e) {
            log.error("[ConsumerRegistration] 스트림 리스너 등록 실패: stream={}, consumerGroup={}",
                streamKey, consumerGroup, e);
            throw new ConsumerRegistrationException("Stream Listener 등록에 실패했습니다: " + streamKey, e);
        }
    }

    /**
     * Consumer가 특정 스트림을 처리해야 하는지 판단
     */
    private boolean shouldProcessStream(String streamKey, MessageConsumer consumer) {
        String consumerType = consumer.getClass().getSimpleName();

        switch (consumerType) {
            case "RoomBroadcastConsumer":
                return streamKey.startsWith("stream:room:");
            case "UserReadStateConsumer":
                return streamKey.startsWith("stream:user:");
            case "NotificationConsumer":
                return streamKey.startsWith("stream:user:") || streamKey.equals("stream:notifications");
            case "MessageStorageConsumer":
                return true; // 모든 스트림 처리
            default:
                log.warn("[ConsumerRegistration] 알 수 없는 Consumer 타입: {}", consumerType);
                return false;
        }
    }

    /**
     * 방 스트림을 Consumer Group에서 제거
     */
    public void removeRoomStreamFromConsumerGroups(String streamKey) {
        try {
            log.info("[ConsumerRegistration] 방 스트림 Consumer Group 제거 시작: streamKey={}", streamKey);

            // 실제 운영에서는 스트림 제거보다는 Consumer Group 정지를 고려
            // 현재는 로깅만 수행
            log.info("[ConsumerRegistration] 방 스트림 Consumer Group 제거 완료: streamKey={}", streamKey);

        } catch (Exception e) {
            log.error("[ConsumerRegistration] 방 스트림 Consumer Group 제거 실패: streamKey={}", streamKey, e);
        }
    }

    /**
     * 모든 Consumer Group 상태 조회
     */
    public ConsumerGroupStats getConsumerGroupStats() {
        try {
            boolean containerRunning = listenerContainer.isRunning();
            boolean consumersHealthy = roomBroadcastConsumer.isHealthy() &&
                                     userReadStateConsumer.isHealthy() &&
                                     messageStorageConsumer.isHealthy() &&
                                     notificationConsumer.isHealthy();

            return new ConsumerGroupStats(containerRunning, consumersHealthy);

        } catch (Exception e) {
            log.error("[ConsumerRegistration] Consumer Group 상태 조회 실패", e);
            return new ConsumerGroupStats(false, false);
        }
    }

    /**
     * Consumer Group 통계 클래스
     */
    public static class ConsumerGroupStats {
        private final boolean containerRunning;
        private final boolean consumersHealthy;

        public ConsumerGroupStats(boolean containerRunning, boolean consumersHealthy) {
            this.containerRunning = containerRunning;
            this.consumersHealthy = consumersHealthy;
        }

        public boolean isContainerRunning() { return containerRunning; }
        public boolean areConsumersHealthy() { return consumersHealthy; }
        public boolean isOverallHealthy() { return containerRunning && consumersHealthy; }

        @Override
        public String toString() {
            return String.format("ConsumerGroupStats{container=%s, consumers=%s, overall=%s}",
                containerRunning ? "RUNNING" : "STOPPED",
                consumersHealthy ? "HEALTHY" : "UNHEALTHY",
                isOverallHealthy() ? "OK" : "FAIL"
            );
        }
    }
}