package com.vibechat.consumer.notification;

import com.vibechat.consumer.core.AbstractMessageConsumer;
import com.vibechat.consumer.core.ConsumerProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 오프라인 사용자 알림 Consumer
 *
 * 책임: stream:user:* 스트림을 구독하여 오프라인 사용자에게 알림 발송
 * SRP: 알림 발송만 담당
 * 확장성: FCM, 이메일, SMS 등 다양한 알림 채널 지원 가능
 */
@Component
@Slf4j
public class NotificationConsumer extends AbstractMessageConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    // TODO: 실제 구현에서는 FCM, Email 서비스 등을 주입받아야 함
    // private final FcmNotificationService fcmNotificationService;
    // private final EmailNotificationService emailNotificationService;

    private static final Set<String> SUPPORTED_TYPES = Set.of(
        "TEXT", "IMAGE", "GIF", "VIDEO", "FILE"  // SYSTEM 메시지는 알림 제외
    );

    // Redis 키 패턴
    private static final String USER_PRESENCE_KEY = "presence:user:{}";
    private static final String NOTIFICATION_QUEUE_KEY = "notifications:user:{}";
    private static final String NOTIFICATION_SETTINGS_KEY = "settings:user:{}:notifications";

    // 알림 상수
    private static final int NOTIFICATION_TTL_HOURS = 72;  // 72시간 후 알림 만료
    private static final int MAX_PENDING_NOTIFICATIONS = 100;  // 사용자당 최대 대기 알림

    public NotificationConsumer(RedisTemplate<String, Object> redisTemplate) {
        super("NotificationConsumer", SUPPORTED_TYPES);
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void processBusinessLogic(MapRecord<String, String, Object> record) throws ConsumerProcessingException {
        String messageId = record.getId().getValue();
        Map<String, Object> messageData = extractMessageData(record);

        try {
            // 1. 필수 필드 검증
            requireField(messageData, "userId", messageId);
            requireField(messageData, "roomId", messageId);
            requireField(messageData, "eventType", messageId);

            // 2. 이벤트 타입 확인 (메시지 수신만 알림 대상)
            String eventType = (String) messageData.get("eventType");
            if (!"MESSAGE_RECEIVED".equals(eventType)) {
                log.debug("[Notification] 알림 대상이 아닌 이벤트: eventType={}, messageId={}",
                    eventType, messageId);
                return;
            }

            // 3. 메시지 데이터 추출
            Long userId = Long.parseLong(messageData.get("userId").toString());
            Long roomId = Long.parseLong(messageData.get("roomId").toString());
            String senderId = messageData.get("senderId") != null
                ? messageData.get("senderId").toString()
                : messageData.get("userId").toString();

            // 4. 자신이 보낸 메시지는 알림 제외
            if (userId.toString().equals(senderId)) {
                log.debug("[Notification] 자신이 보낸 메시지는 알림 제외: userId={}, messageId={}",
                    userId, messageId);
                return;
            }

            // 5. 사용자 온라인 상태 확인
            if (isUserOnline(userId)) {
                log.debug("[Notification] 온라인 사용자는 알림 제외: userId={}, messageId={}",
                    userId, messageId);
                return;
            }

            // 6. 알림 설정 확인
            if (!isNotificationEnabled(userId, roomId)) {
                log.debug("[Notification] 알림이 비활성화된 사용자: userId={}, roomId={}, messageId={}",
                    userId, roomId, messageId);
                return;
            }

            // 7. 알림 생성 및 발송
            processNotification(userId, roomId, messageId, messageData);

            log.info("[Notification] 오프라인 사용자 알림 처리 완료: userId={}, roomId={}, messageId={}",
                userId, roomId, messageId);

        } catch (NumberFormatException e) {
            throw new ConsumerProcessingException(getConsumerType(), messageId,
                "잘못된 숫자 형식의 userId 또는 roomId", e, false);
        } catch (Exception e) {
            throw new ConsumerProcessingException(getConsumerType(), messageId,
                "알림 처리 중 오류 발생", e);
        }
    }

    /**
     * 사용자 온라인 상태 확인
     */
    private boolean isUserOnline(Long userId) {
        try {
            String presenceKey = USER_PRESENCE_KEY.replace("{}", userId.toString());
            return redisTemplate.hasKey(presenceKey);
        } catch (Exception e) {
            log.warn("[Notification] 사용자 온라인 상태 확인 실패: userId={}", userId, e);
            return false; // 오류 시 오프라인으로 간주하여 알림 발송
        }
    }

    /**
     * 사용자 알림 설정 확인
     */
    private boolean isNotificationEnabled(Long userId, Long roomId) {
        try {
            String settingsKey = NOTIFICATION_SETTINGS_KEY.replace("{}", userId.toString());

            // 기본값: 알림 활성화
            Object settings = redisTemplate.opsForHash().get(settingsKey, "room:" + roomId);
            if (settings == null) {
                settings = redisTemplate.opsForHash().get(settingsKey, "global");
            }

            return settings == null || Boolean.parseBoolean(settings.toString());
        } catch (Exception e) {
            log.warn("[Notification] 알림 설정 확인 실패: userId={}, roomId={}", userId, roomId, e);
            return true; // 오류 시 알림 활성화로 간주
        }
    }

    /**
     * 알림 생성 및 발송 처리
     */
    private void processNotification(Long userId, Long roomId, String messageId, Map<String, Object> messageData) {
        try {
            // 1. 알림 메타데이터 생성
            NotificationData notification = buildNotificationData(userId, roomId, messageId, messageData);

            // 2. 대기 중인 알림 수 확인
            if (getPendingNotificationCount(userId) >= MAX_PENDING_NOTIFICATIONS) {
                log.warn("[Notification] 사용자 알림 큐 가득참: userId={}, 가장 오래된 알림 삭제", userId);
                removeOldestNotification(userId);
            }

            // 3. 알림을 Redis 큐에 저장 (오프라인 시 재발송용)
            storeNotificationInQueue(userId, notification);

            // 4. 실제 알림 발송 시도
            sendNotification(userId, notification);

        } catch (Exception e) {
            log.error("[Notification] 알림 처리 실패: userId={}, messageId={}", userId, messageId, e);
            throw new ConsumerProcessingException(getConsumerType(), messageId,
                "알림 발송 실패", e);
        }
    }

    /**
     * 알림 데이터 구성
     */
    private NotificationData buildNotificationData(Long userId, Long roomId, String messageId, Map<String, Object> messageData) {
        NotificationData notification = new NotificationData();

        notification.setUserId(userId);
        notification.setRoomId(roomId);
        notification.setMessageId(messageId);

        // 발신자 정보
        String senderId = messageData.get("senderId") != null
            ? messageData.get("senderId").toString()
            : "Unknown";
        notification.setSenderId(senderId);
        notification.setSenderName("사용자 " + senderId); // TODO: 실제 닉네임 조회

        // 메시지 내용
        String messageType = (String) messageData.get("messageType");
        String content = (String) messageData.get("content");

        notification.setMessageType(messageType);
        notification.setTitle(buildNotificationTitle(notification.getSenderName()));
        notification.setBody(buildNotificationBody(messageType, content));

        // 메타데이터
        notification.setRoomName("방 " + roomId); // TODO: 실제 방 이름 조회
        notification.setCreatedAt(LocalDateTime.now());

        return notification;
    }

    /**
     * 알림 제목 생성
     */
    private String buildNotificationTitle(String senderName) {
        return senderName + "님의 새 메시지";
    }

    /**
     * 알림 본문 생성
     */
    private String buildNotificationBody(String messageType, String content) {
        switch (messageType) {
            case "TEXT":
                return content != null && content.length() > 50
                    ? content.substring(0, 50) + "..."
                    : content;
            case "IMAGE":
                return "📷 사진을 보냈습니다";
            case "GIF":
                return "🎞️ GIF를 보냈습니다";
            case "VIDEO":
                return "🎥 동영상을 보냈습니다";
            case "FILE":
                return "📎 파일을 보냈습니다";
            default:
                return "새 메시지가 도착했습니다";
        }
    }

    /**
     * 사용자의 대기 중인 알림 수 조회
     */
    private long getPendingNotificationCount(Long userId) {
        try {
            String queueKey = NOTIFICATION_QUEUE_KEY.replace("{}", userId.toString());
            return redisTemplate.opsForList().size(queueKey);
        } catch (Exception e) {
            log.warn("[Notification] 대기 알림 수 조회 실패: userId={}", userId, e);
            return 0;
        }
    }

    /**
     * 가장 오래된 알림 제거
     */
    private void removeOldestNotification(Long userId) {
        try {
            String queueKey = NOTIFICATION_QUEUE_KEY.replace("{}", userId.toString());
            redisTemplate.opsForList().leftPop(queueKey);
        } catch (Exception e) {
            log.warn("[Notification] 오래된 알림 제거 실패: userId={}", userId, e);
        }
    }

    /**
     * 알림을 Redis 큐에 저장
     */
    private void storeNotificationInQueue(Long userId, NotificationData notification) {
        try {
            String queueKey = NOTIFICATION_QUEUE_KEY.replace("{}", userId.toString());

            // JSON 직렬화 (간단 구현)
            String notificationJson = notification.toJsonString();

            redisTemplate.opsForList().rightPush(queueKey, notificationJson);
            redisTemplate.expire(queueKey, NOTIFICATION_TTL_HOURS, TimeUnit.HOURS);

        } catch (Exception e) {
            log.error("[Notification] 알림 큐 저장 실패: userId={}", userId, e);
        }
    }

    /**
     * 실제 알림 발송
     */
    private void sendNotification(Long userId, NotificationData notification) {
        try {
            // TODO: 실제 구현에서는 FCM, 이메일 등으로 발송
            // fcmNotificationService.sendNotification(userId, notification);
            // emailNotificationService.sendNotification(userId, notification);

            log.info("[Notification] 알림 발송 시도: userId={}, title={}, body={}",
                userId, notification.getTitle(), notification.getBody());

            // Mock 구현: 로그로 대체
            log.info("[Notification] 📱 Push 알림 발송됨: {}", notification.getTitle());

        } catch (Exception e) {
            log.error("[Notification] 알림 발송 실패: userId={}", userId, e);
            // 발송 실패해도 큐에는 저장되어 있으므로 재시도 가능
        }
    }

    /**
     * 사용자 온라인 시 대기 중인 알림 처리
     */
    public void processPendingNotifications(Long userId) {
        try {
            String queueKey = NOTIFICATION_QUEUE_KEY.replace("{}", userId.toString());

            while (redisTemplate.opsForList().size(queueKey) > 0) {
                String notificationJson = (String) redisTemplate.opsForList().leftPop(queueKey);
                if (notificationJson != null) {
                    // TODO: JSON 역직렬화 후 재발송
                    log.info("[Notification] 대기 중인 알림 처리: userId={}", userId);
                }
            }

        } catch (Exception e) {
            log.error("[Notification] 대기 알림 처리 실패: userId={}", userId, e);
        }
    }

    /**
     * 알림 설정 업데이트
     */
    public void updateNotificationSettings(Long userId, Long roomId, boolean enabled) {
        try {
            String settingsKey = NOTIFICATION_SETTINGS_KEY.replace("{}", userId.toString());
            redisTemplate.opsForHash().put(settingsKey, "room:" + roomId, String.valueOf(enabled));
            redisTemplate.expire(settingsKey, 30, TimeUnit.DAYS);

            log.info("[Notification] 알림 설정 업데이트: userId={}, roomId={}, enabled={}",
                userId, roomId, enabled);

        } catch (Exception e) {
            log.error("[Notification] 알림 설정 업데이트 실패: userId={}, roomId={}", userId, roomId, e);
        }
    }

    /**
     * 알림 시스템 헬스체크
     */
    @Override
    public boolean isHealthy() {
        try {
            // Redis 연결 상태 확인
            redisTemplate.opsForValue().get("health:check");
            return true;
        } catch (Exception e) {
            log.error("[Notification] 헬스체크 실패: Redis 연결 불가", e);
            return false;
        }
    }

    /**
     * 알림 데이터 클래스
     */
    private static class NotificationData {
        private Long userId;
        private Long roomId;
        private String messageId;
        private String senderId;
        private String senderName;
        private String messageType;
        private String title;
        private String body;
        private String roomName;
        private LocalDateTime createdAt;

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Long getRoomId() { return roomId; }
        public void setRoomId(Long roomId) { this.roomId = roomId; }

        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }

        public String getSenderId() { return senderId; }
        public void setSenderId(String senderId) { this.senderId = senderId; }

        public String getSenderName() { return senderName; }
        public void setSenderName(String senderName) { this.senderName = senderName; }

        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }

        public String getRoomName() { return roomName; }
        public void setRoomName(String roomName) { this.roomName = roomName; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public String toJsonString() {
            // 간단한 JSON 직렬화 (실제로는 Jackson ObjectMapper 사용 권장)
            return String.format("{\"userId\":%d,\"title\":\"%s\",\"body\":\"%s\",\"createdAt\":\"%s\"}",
                userId, title, body, createdAt.toString());
        }
    }
}