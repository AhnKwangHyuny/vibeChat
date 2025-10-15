package com.vibechat.service.streams.consumer;

import com.vibechat.consumer.core.MessageConsumer;
import com.vibechat.consumer.room.RoomBroadcastConsumer;
import com.vibechat.consumer.user.UserReadStateConsumer;
import com.vibechat.consumer.storage.MessageStorageConsumer;
import com.vibechat.consumer.notification.NotificationConsumer;
import com.vibechat.service.streams.router.StreamMessageRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Consumer Group 등록 관리 서비스 구현체
 *
 * 핵심 전략: Lazy Container Start Pattern
 * - Container는 생성만 하고 시작하지 않음
 * - 첫 Listener 등록 시점에 Container 시작 (ensureContainerStarted)
 * - Listener가 없는 Container는 메시지를 폴링하지 않음을 방지
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsumerRegistrationServiceImpl implements ConsumerRegistrationService {

    // Consumer Group 상수 정의
    private static final String BROADCAST_GROUP = "broadcast-group";
    private static final String STORAGE_GROUP = "storage-group";
    private static final String READSTATE_GROUP = "readstate-group";
    private static final String NOTIFICATION_GROUP = "notification-group";

    private final StreamMessageListenerContainer<String, MapRecord<String, String, Object>> listenerContainer;
    private final RoomBroadcastConsumer roomBroadcastConsumer;
    private final UserReadStateConsumer userReadStateConsumer;
    private final MessageStorageConsumer messageStorageConsumer;
    private final NotificationConsumer notificationConsumer;
    private final StreamMessageRouter streamMessageRouter;
    private final RedisTemplate<String, Object> redisTemplate;

    private StreamListener<String, MapRecord<String, String, Object>> universalListener;
    private final AtomicBoolean containerStarted = new AtomicBoolean(false);

    @Override
    public void addRoomStreamToConsumerGroups(String streamKey) {
        try {
            log.info("[ConsumerRegistration] 방 스트림 등록 시작: streamKey={}", streamKey);

            //  Container 시작 보장
            ensureContainerStarted();

            // 2. Consumer Group 및 Listener 등록
            registerStreamForConsumer(streamKey, BROADCAST_GROUP, "broadcast-consumer-1", roomBroadcastConsumer);
            registerStreamForConsumer(streamKey, STORAGE_GROUP, "storage-consumer-1", messageStorageConsumer);

            // 3. Router에 스트림 등록
            streamMessageRouter.registerStream(streamKey);

            log.info("[ConsumerRegistration] 방 스트림 등록 완료: streamKey={}", streamKey);

        } catch (Exception e) {
            log.error("[ConsumerRegistration] 방 스트림 등록 실패: streamKey={}", streamKey, e);
            throw new ConsumerRegistrationException("방 스트림 등록 실패: " + streamKey, e);
        }
    }

    @Override
    public void addUserStreamToConsumerGroups(String streamKey) {
        log.debug("[ConsumerRegistration] User Stream 등록 스킵 (현재 미사용): streamKey={}", streamKey);
        // TODO: User Stream 기능 활성화 시 구현 필요
    }

    @Override
    public void removeRoomStreamFromConsumerGroups(String streamKey) {
        try {
            log.info("[ConsumerRegistration] 방 스트림 제거 시작: streamKey={}", streamKey);

            streamMessageRouter.unregisterStream(streamKey);

            log.info("[ConsumerRegistration] 방 스트림 제거 완료: streamKey={}", streamKey);

        } catch (Exception e) {
            log.error("[ConsumerRegistration] 방 스트림 제거 실패: streamKey={}", streamKey, e);
        }
    }

    @Override
    public ConsumerGroupStats getConsumerGroupStats() {
        try {
            boolean containerRunning = listenerContainer.isRunning();
            boolean consumersHealthy = isAllConsumersHealthy();

            return new ConsumerGroupStats(containerRunning, consumersHealthy);

        } catch (Exception e) {
            log.error("[ConsumerRegistration] Consumer Group 상태 조회 실패", e);
            return new ConsumerGroupStats(false, false);
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * Container 시작 보장 (Lazy Start Pattern 핵심)
     *
     * 첫 Listener 등록 시점에만 Container 시작
     * - AtomicBoolean로 동시성 안전 보장
     * - 실패 시 플래그 복구로 재시도 가능
     */
    private void ensureContainerStarted() {
        if (containerStarted.compareAndSet(false, true)) {
            try {
                listenerContainer.start();
                log.info("[ConsumerRegistration] Container 시작 완료 (첫 Listener 등록 시점)");
            } catch (Exception e) {
                containerStarted.set(false);
                log.error("[ConsumerRegistration] Container 시작 실패!", e);
                throw new IllegalStateException("Container 시작 실패", e);
            }
        }
    }

    /**
     * Universal Listener 초기화 (Lazy Initialization)
     *
     * StreamMessageRouter로 메시지 라우팅 위임
     */
    private StreamListener<String, MapRecord<String, String, Object>> getUniversalListener() {
        if (universalListener == null) {
            universalListener = streamMessageRouter::routeMessage;
        }
        return universalListener;
    }

    /**
     * 스트림을 Universal Listener로 등록
     *
     * 메모리 최적화: 모든 스트림이 하나의 Listener 공유
     */
    private void registerStreamForConsumer(String streamKey, String consumerGroup, String consumerId,
                                         MessageConsumer consumer) {
        try {
            // 1. Consumer Group 생성 (이미 존재하면 무시)
            createConsumerGroupIfNotExists(streamKey, consumerGroup);

            // 2. Redis Consumer 정의
            Consumer redisConsumer = Consumer.from(consumerGroup, consumerId);

            // 3. StreamOffset 설정 (마지막 소비 지점부터 읽기)
            StreamOffset<String> streamOffset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());

            // 4. Container에 Subscription 등록
            listenerContainer.receiveAutoAck(redisConsumer, streamOffset, getUniversalListener());

            log.info("[ConsumerRegistration] Listener 등록 완료: stream={}, group={}, consumer={}",
                streamKey, consumerGroup, consumer.getClass().getSimpleName());

        } catch (Exception e) {
            log.error("[ConsumerRegistration] Listener 등록 실패: stream={}, group={}",
                streamKey, consumerGroup, e);
            throw new ConsumerRegistrationException("Listener 등록 실패: " + streamKey, e);
        }
    }

    /**
     * Consumer Group 생성 (이미 존재하면 무시)
     *
     * ReadOffset.latest(): 입장 시점 이후 메시지만 처리
     */
    private void createConsumerGroupIfNotExists(String streamKey, String consumerGroup) {
        try {
            redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.latest(), consumerGroup);
            log.info("[ConsumerRegistration] Consumer Group 생성: stream={}, group={}", streamKey, consumerGroup);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.debug("[ConsumerRegistration] Consumer Group 이미 존재: stream={}, group={}", 
                    streamKey, consumerGroup);
            } else {
                log.warn("[ConsumerRegistration] Consumer Group 생성 오류 (무시): stream={}, group={}, error={}",
                    streamKey, consumerGroup, e.getMessage());
            }
        }
    }

    /**
     * 모든 Consumer의 Health 상태 확인
     */
    private boolean isAllConsumersHealthy() {
        return roomBroadcastConsumer.isHealthy()
            && userReadStateConsumer.isHealthy()
            && messageStorageConsumer.isHealthy()
            && notificationConsumer.isHealthy();
    }
}