package com.vibechat.service.streams.router;

import com.vibechat.consumer.room.RoomBroadcastConsumer;
import com.vibechat.consumer.user.UserReadStateConsumer;
import com.vibechat.consumer.storage.MessageStorageConsumer;
import com.vibechat.consumer.notification.NotificationConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis Streams 메시지 라우터 구현체
 *
 * 책임: 스트림 패턴에 따라 적절한 Consumer로 메시지 라우팅
 * - stream:room:* → RoomBroadcastConsumer + MessageStorageConsumer
 * - stream:user:* → UserReadStateConsumer + MessageStorageConsumer + NotificationConsumer
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StreamMessageRouterImpl implements StreamMessageRouter {

    private final RoomBroadcastConsumer roomBroadcastConsumer;
    private final UserReadStateConsumer userReadStateConsumer;
    private final MessageStorageConsumer messageStorageConsumer;
    private final NotificationConsumer notificationConsumer;

    private final Set<String> registeredStreams = ConcurrentHashMap.newKeySet();

    @Override
    public void routeMessage(MapRecord<String, String, Object> record) {
        String streamKey = record.getStream();

        try {
            // 등록된 스트림인지 확인 (성능 최적화)
            if (!registeredStreams.contains(streamKey)) {
                log.debug("[Router] 미등록 스트림 무시: {}", streamKey);
                return;
            }

            // 스트림 패턴에 따라 라우팅
            if (streamKey.startsWith("stream:room:")) {
                routeRoomMessage(record);
            } else if (streamKey.startsWith("stream:user:")) {
                routeUserMessage(record);
            } else {
                log.debug("[Router] 알 수 없는 스트림 패턴: {}", streamKey);
            }

        } catch (Exception e) {
            log.error("[Router] 메시지 라우팅 실패: stream={}, messageId={}",
                streamKey, record.getId().getValue(), e);
        }
    }

    @Override
    public void routeRoomMessage(MapRecord<String, String, Object> record) {
        String messageId = record.getId().getValue();

        // 1. RoomBroadcastConsumer 처리 (WebSocket 브로드캐스트)
        try {
            roomBroadcastConsumer.processMessage(record);
        } catch (Exception e) {
            log.error("[Router] RoomBroadcast 처리 실패: messageId={}", messageId, e);
        }

        // 2. MessageStorageConsumer 처리 (MongoDB 저장)
        try {
            messageStorageConsumer.processMessage(record);
        } catch (Exception e) {
            log.error("[Router] MessageStorage 처리 실패: messageId={}", messageId, e);
        }
    }

    @Override
    public void routeUserMessage(MapRecord<String, String, Object> record) {
        String messageId = record.getId().getValue();

        // 1. UserReadStateConsumer 처리 (읽음 상태 관리)
        try {
            userReadStateConsumer.processMessage(record);
        } catch (Exception e) {
            log.error("[Router] UserReadState 처리 실패: messageId={}", messageId, e);
        }

        // 2. MessageStorageConsumer 처리 (MongoDB 저장)
        try {
            messageStorageConsumer.processMessage(record);
        } catch (Exception e) {
            log.error("[Router] MessageStorage 처리 실패: messageId={}", messageId, e);
        }

        // 3. NotificationConsumer 처리 (오프라인 알림)
        try {
            notificationConsumer.processMessage(record);
        } catch (Exception e) {
            log.error("[Router] Notification 처리 실패: messageId={}", messageId, e);
        }
    }

    @Override
    public boolean isRegisteredStream(String streamKey) {
        return registeredStreams.contains(streamKey);
    }

    /**
     * 스트림 등록 (ConsumerRegistrationService에서 호출)
     */
    @Override
    public void registerStream(String streamKey) {
        registeredStreams.add(streamKey);
        log.debug("[Router] 스트림 등록: {}", streamKey);
    }

    /**
     * 스트림 제거 (ConsumerRegistrationService에서 호출)
     */
    @Override
    public void unregisterStream(String streamKey) {
        registeredStreams.remove(streamKey);
        log.debug("[Router] 스트림 제거: {}", streamKey);
    }

    /**
     * 등록된 스트림 목록 조회
     */
    public Set<String> getRegisteredStreams() {
        return Set.copyOf(registeredStreams);
    }
}