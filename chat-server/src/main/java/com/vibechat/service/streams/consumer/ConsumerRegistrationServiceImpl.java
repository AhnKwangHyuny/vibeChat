package com.vibechat.service.streams.consumer;

import com.vibechat.consumer.core.MessageConsumer;
import com.vibechat.consumer.room.RoomBroadcastConsumer;
import com.vibechat.consumer.user.UserReadStateConsumer;
import com.vibechat.consumer.storage.MessageStorageConsumer;
import com.vibechat.consumer.notification.NotificationConsumer;
import com.vibechat.service.streams.router.StreamMessageRouter;
import com.vibechat.service.streams.router.StreamMessageRouterImpl;
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
 * Consumer Group 등록 관리 서비스 구현체
 *
 * 책임: Consumer Group 생명주기 관리만 담당 (SRP 준수)
 * - StreamMessageRouter에게 라우팅 책임 위임 (DIP 준수)
 * - 새로운 Consumer 추가 시 확장 용이 (OCP 준수)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsumerRegistrationServiceImpl implements ConsumerRegistrationService {

    private final StreamMessageListenerContainer<String, ObjectRecord<String, Object>> listenerContainer;
    private final RoomBroadcastConsumer roomBroadcastConsumer;
    private final UserReadStateConsumer userReadStateConsumer;
    private final MessageStorageConsumer messageStorageConsumer;
    private final NotificationConsumer notificationConsumer;
    private final StreamMessageRouter streamMessageRouter;

    private StreamListener<String, ObjectRecord<String, Object>> universalListener;

    /**
     * 의존성 주입 후 초기화
     */
    private StreamListener<String, ObjectRecord<String, Object>> getUniversalListener() {
        if (universalListener == null) {
            universalListener = streamMessageRouter::routeMessage;
        }
        return universalListener;
    }

    @Override
    public void addRoomStreamToConsumerGroups(String streamKey) {
        try {
            log.info("[ConsumerRegistration] 방 스트림 Consumer Group 등록 시작: streamKey={}", streamKey);

            // 1. RoomBroadcastConsumer에 방 스트림 추가
            registerStreamForConsumer(streamKey, "broadcast-group", "broadcast-consumer-1", roomBroadcastConsumer);

            // 2. MessageStorageConsumer에 방 스트림 추가 (모든 스트림 처리)
            registerStreamForConsumer(streamKey, "storage-group", "storage-consumer-1", messageStorageConsumer);

            // 3. Router에 스트림 등록
            ((StreamMessageRouterImpl) streamMessageRouter).registerStream(streamKey);

            log.info("[ConsumerRegistration] 방 스트림 Consumer Group 등록 완료: streamKey={}", streamKey);

        } catch (Exception e) {
            log.error("[ConsumerRegistration] 방 스트림 Consumer Group 등록 실패: streamKey={}", streamKey, e);
            throw new ConsumerRegistrationException("방 스트림 Consumer Group 등록에 실패했습니다", e);
        }
    }

    @Override
    public void addUserStreamToConsumerGroups(String streamKey) {
        // TODO: 읽음 상태 기능 구현 시 활성화
        // 현재는 Room Stream만 사용하므로 비활성화
        log.debug("[ConsumerRegistration] User Stream 등록 스킵 (읽음 상태 미구현): streamKey={}", streamKey);

        /*
        try {
            log.info("[ConsumerRegistration] 사용자 스트림 Consumer Group 등록 시작: streamKey={}", streamKey);

            // 1. UserReadStateConsumer에 사용자 스트림 추가
            registerStreamForConsumer(streamKey, "readstate-group", "readstate-consumer-1", userReadStateConsumer);

            // 2. MessageStorageConsumer에 사용자 스트림 추가 (모든 스트림 처리)
            registerStreamForConsumer(streamKey, "storage-group", "storage-consumer-1", messageStorageConsumer);

            // 3. NotificationConsumer에 사용자 스트림 추가
            registerStreamForConsumer(streamKey, "notification-group", "notification-consumer-1", notificationConsumer);

            // 4. Router에 스트림 등록
            ((StreamMessageRouterImpl) streamMessageRouter).registerStream(streamKey);

            log.info("[ConsumerRegistration] 사용자 스트림 Consumer Group 등록 완료: streamKey={}", streamKey);

        } catch (Exception e) {
            log.error("[ConsumerRegistration] 사용자 스트림 Consumer Group 등록 실패: streamKey={}", streamKey, e);
            throw new ConsumerRegistrationException("사용자 스트림 Consumer Group 등록에 실패했습니다", e);
        }
        */
    }

    @Override
    public void removeRoomStreamFromConsumerGroups(String streamKey) {
        try {
            log.info("[ConsumerRegistration] 방 스트림 Consumer Group 제거 시작: streamKey={}", streamKey);

            // Router에서 스트림 제거
            ((StreamMessageRouterImpl) streamMessageRouter).unregisterStream(streamKey);

            log.info("[ConsumerRegistration] 방 스트림 Consumer Group 제거 완료: streamKey={}", streamKey);

        } catch (Exception e) {
            log.error("[ConsumerRegistration] 방 스트림 Consumer Group 제거 실패: streamKey={}", streamKey, e);
        }
    }

    @Override
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
     * 스트림을 고정 Universal Listener로 등록 (메모리 최적화)
     */
    private void registerStreamForConsumer(String streamKey, String consumerGroup, String consumerId,
                                         MessageConsumer consumer) {
        try {
            // Redis Consumer 정의
            Consumer redisConsumer = Consumer.from(consumerGroup, consumerId);

            // 고정 Universal Listener 사용 (기존 동적 생성 제거)
            StreamOffset<String> streamOffset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());

            // Container에 Subscription 등록
            listenerContainer.receiveAutoAck(redisConsumer, streamOffset, getUniversalListener());

            log.debug("[ConsumerRegistration] 스트림 리스너 등록 완료: stream={}, consumer={}, group={}",
                streamKey, consumer.getClass().getSimpleName(), consumerGroup);

        } catch (Exception e) {
            log.error("[ConsumerRegistration] 스트림 리스너 등록 실패: stream={}, consumerGroup={}",
                streamKey, consumerGroup, e);
            throw new ConsumerRegistrationException("Stream Listener 등록에 실패했습니다: " + streamKey, e);
        }
    }
}