package com.vibechat.consumer.user;

import com.vibechat.consumer.core.AbstractMessageConsumer;
import com.vibechat.consumer.core.ConsumerProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 사용자 읽음 상태 추적 Consumer
 *
 * 책임: stream:user:* 스트림을 구독하여 읽음 상태 및 알림 카운트 관리
 * SRP: 읽음 상태 추적만 담당
 * 카톡식 안읽은 메시지 수 계산의 핵심 컴포넌트
 */
@Component
@Slf4j
public class UserReadStateConsumer extends AbstractMessageConsumer {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final Set<String> SUPPORTED_TYPES = Set.of(
        "TEXT", "IMAGE", "GIF", "VIDEO", "FILE"  // SYSTEM 메시지는 읽음 처리 제외
    );

    // Redis 키 패턴
    private static final String USER_LAST_READ_KEY = "user:{}:room:{}:last_read";
    private static final String USER_UNREAD_COUNT_KEY = "user:{}:room:{}:unread_count";
    private static final String ROOM_MESSAGE_COUNTER_KEY = "room:{}:message_counter";

    public UserReadStateConsumer(RedisTemplate<String, Object> redisTemplate) {
        super("UserReadStateConsumer", SUPPORTED_TYPES);
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void processBusinessLogic(ObjectRecord<String, Object> record) throws ConsumerProcessingException {
        String messageId = record.getId().getValue();
        Map<String, Object> messageData = extractMessageData(record);

        try {
            // 1. 필수 필드 검증
            requireField(messageData, "userId", messageId);
            requireField(messageData, "roomId", messageId);
            requireField(messageData, "eventType", messageId);

            // 2. 메시지 데이터 추출
            Long userId = Long.parseLong(messageData.get("userId").toString());
            Long roomId = Long.parseLong(messageData.get("roomId").toString());
            String eventType = (String) messageData.get("eventType");

            // 3. 이벤트 타입별 처리
            if ("MESSAGE_RECEIVED".equals(eventType)) {
                processMessageReceived(userId, roomId, messageId, messageData);
            } else if ("MESSAGE_READ".equals(eventType)) {
                processMessageRead(userId, roomId, messageId);
            } else {
                log.debug("[UserReadState] 알 수 없는 이벤트 타입: eventType={}, messageId={}",
                    eventType, messageId);
            }

            log.debug("[UserReadState] 읽음 상태 처리 완료: userId={}, roomId={}, eventType={}",
                userId, roomId, eventType);

        } catch (NumberFormatException e) {
            throw new ConsumerProcessingException(getConsumerType(), messageId,
                "잘못된 숫자 형식의 userId 또는 roomId", e, false);
        }
    }

    /**
     * 메시지 수신 처리 (안읽은 메시지 카운트 증가)
     */
    private void processMessageReceived(Long userId, Long roomId, String messageId, Map<String, Object> messageData) {
        try {
            // 1. 발송자는 읽음 처리 (자신이 보낸 메시지)
            String senderId = messageData.get("senderId") != null
                ? messageData.get("senderId").toString()
                : messageData.get("userId").toString();

            if (userId.toString().equals(senderId)) {
                // 자신이 보낸 메시지는 즉시 읽음 처리
                updateLastReadMessage(userId, roomId, messageId);
                log.debug("[UserReadState] 발송자 자동 읽음 처리: userId={}, roomId={}, messageId={}",
                    userId, roomId, messageId);
            } else {
                // 다른 사람이 보낸 메시지는 안읽은 카운트 증가
                incrementUnreadCount(userId, roomId);
                log.debug("[UserReadState] 안읽은 메시지 카운트 증가: userId={}, roomId={}",
                    userId, roomId);
            }

            // 2. 방 전체 메시지 카운터 증가 (읽음 상태 계산용)
            incrementRoomMessageCounter(roomId);

        } catch (Exception e) {
            throw new ConsumerProcessingException(getConsumerType(), messageId,
                "메시지 수신 처리 중 오류 발생", e);
        }
    }

    /**
     * 메시지 읽음 처리 (안읽은 메시지 카운트 초기화)
     */
    private void processMessageRead(Long userId, Long roomId, String messageId) {
        try {
            // 1. 마지막 읽은 메시지 ID 업데이트
            updateLastReadMessage(userId, roomId, messageId);

            // 2. 안읽은 메시지 카운트 초기화
            resetUnreadCount(userId, roomId);

            log.info("[UserReadState] 메시지 읽음 처리 완료: userId={}, roomId={}, lastReadId={}",
                userId, roomId, messageId);

        } catch (Exception e) {
            throw new ConsumerProcessingException(getConsumerType(), messageId,
                "메시지 읽음 처리 중 오류 발생", e);
        }
    }

    /**
     * 마지막 읽은 메시지 ID 업데이트
     */
    private void updateLastReadMessage(Long userId, Long roomId, String messageId) {
        String key = USER_LAST_READ_KEY
            .replace("{}", userId.toString())
            .replace("{}", roomId.toString());

        redisTemplate.opsForValue().set(key, messageId, 30, TimeUnit.DAYS);
    }

    /**
     * 안읽은 메시지 카운트 증가
     */
    private void incrementUnreadCount(Long userId, Long roomId) {
        String key = USER_UNREAD_COUNT_KEY
            .replace("{}", userId.toString())
            .replace("{}", roomId.toString());

        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 30, TimeUnit.DAYS);
    }

    /**
     * 안읽은 메시지 카운트 초기화
     */
    private void resetUnreadCount(Long userId, Long roomId) {
        String key = USER_UNREAD_COUNT_KEY
            .replace("{}", userId.toString())
            .replace("{}", roomId.toString());

        redisTemplate.delete(key);
    }

    /**
     * 방 전체 메시지 카운터 증가
     */
    private void incrementRoomMessageCounter(Long roomId) {
        String key = ROOM_MESSAGE_COUNTER_KEY.replace("{}", roomId.toString());
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 30, TimeUnit.DAYS);
    }

    /**
     * 사용자의 특정 방 안읽은 메시지 수 조회
     */
    public Long getUnreadCount(Long userId, Long roomId) {
        String key = USER_UNREAD_COUNT_KEY
            .replace("{}", userId.toString())
            .replace("{}", roomId.toString());

        Object count = redisTemplate.opsForValue().get(key);
        return count != null ? Long.parseLong(count.toString()) : 0L;
    }

    /**
     * 특정 메시지의 읽지 않은 사용자 수 계산 (카톡식)
     */
    public int calculateUnreadUserCount(Long roomId, String messageId, java.util.List<Long> roomParticipants) {
        int unreadCount = 0;

        for (Long userId : roomParticipants) {
            String lastReadKey = USER_LAST_READ_KEY
                .replace("{}", userId.toString())
                .replace("{}", roomId.toString());

            String lastReadId = (String) redisTemplate.opsForValue().get(lastReadKey);

            // 마지막 읽은 메시지 ID와 비교하여 읽음 여부 판단
            if (lastReadId == null || isMessageNewer(messageId, lastReadId)) {
                unreadCount++;
            }
        }

        return unreadCount;
    }

    /**
     * 메시지 ID 비교 (타임스탬프 기반)
     */
    private boolean isMessageNewer(String messageId, String lastReadId) {
        try {
            long messageTimestamp = Long.parseLong(messageId.split("-")[0]);
            long lastReadTimestamp = Long.parseLong(lastReadId.split("-")[0]);
            return messageTimestamp > lastReadTimestamp;
        } catch (Exception e) {
            log.warn("메시지 ID 비교 실패: messageId={}, lastReadId={}", messageId, lastReadId);
            return true; // 오류 시 안읽음으로 처리
        }
    }
}