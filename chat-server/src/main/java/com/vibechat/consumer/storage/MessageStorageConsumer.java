package com.vibechat.consumer.storage;

import com.vibechat.consumer.core.AbstractMessageConsumer;
import com.vibechat.consumer.core.ConsumerProcessingException;
import com.vibechat.domain.message.ChatMessage;
import com.vibechat.repository.ChatMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 메시지 영구 저장 Consumer
 *
 * 책임: stream:room:* 및 stream:user:* 스트림을 구독하여 MongoDB에 메시지 영구 저장
 * SRP: 메시지 저장만 담당
 * 성능 최적화: 배치 저장 및 비동기 처리
 */
@Component
@Slf4j
public class MessageStorageConsumer extends AbstractMessageConsumer {

    private final ChatMessageRepository chatMessageRepository;

    private static final Set<String> SUPPORTED_TYPES = Set.of(
        "TEXT", "IMAGE", "GIF", "VIDEO", "FILE", "SYSTEM"
    );

    public MessageStorageConsumer(
        ChatMessageRepository chatMessageRepository) {
        super("MessageStorageConsumer", SUPPORTED_TYPES);
        this.chatMessageRepository = chatMessageRepository;
    }

    @Override
    protected void processBusinessLogic(ObjectRecord<String, Object> record) throws ConsumerProcessingException {
        String messageId = record.getId().getValue();
        Map<String, Object> messageData = extractMessageData(record);

        try {
            // 1. 필수 필드 검증
            requireField(messageData, "roomId", messageId);
            requireField(messageData, "userId", messageId);
            requireField(messageData, "messageType", messageId);
            requireField(messageData, "eventType", messageId);

            // 2. 이벤트 타입 확인 (메시지 수신만 저장)
            String eventType = (String) messageData.get("eventType");
            if (!"MESSAGE_RECEIVED".equals(eventType)) {
                log.debug("[MessageStorage] 저장 대상이 아닌 이벤트: eventType={}, messageId={}",
                    eventType, messageId);
                return;
            }

            // 3. 메시지 데이터 추출
            Long roomId = Long.parseLong(messageData.get("roomId").toString());
            Long userId = Long.parseLong(messageData.get("userId").toString());
            String messageType = (String) messageData.get("messageType");
            String content = (String) messageData.get("content");

            // 4. ChatMessage 엔티티 생성
            ChatMessage chatMessage = buildChatMessageEntity(messageId, messageData, roomId, userId, messageType, content);

            // 5. MongoDB에 저장
            ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

            log.info("[MessageStorage] 메시지 저장 완료: messageId={}, roomId={}, userId={}, dbId={}",
                messageId, roomId, userId, savedMessage.getId());

            // 6. 방별 메시지 제한 관리 (선택적)
            manageRoomMessageLimit(roomId.toString());

        } catch (NumberFormatException e) {
            throw new ConsumerProcessingException(getConsumerType(), messageId,
                "잘못된 숫자 형식의 roomId 또는 userId", e, false);
        } catch (Exception e) {
            throw new ConsumerProcessingException(getConsumerType(), messageId,
                "메시지 저장 중 오류 발생", e);
        }
    }

    /**
     * ChatMessage 엔티티 빌드
     */
    private ChatMessage buildChatMessageEntity(
        String messageId, Map<String, Object> messageData,
        Long roomId, Long userId, String messageType, String content) {

        LocalDateTime timestamp = parseTimestamp((String) messageData.get("timestamp"));
        LocalDateTime now = LocalDateTime.now();

        // 기본 사용자 정보 빌드
        ChatMessage.UserInfo userInfo = ChatMessage.UserInfo.builder()
            .userId(userId)
            .nickname(extractStringField(messageData, "userNickname", "Unknown"))
            .avatarUrl(extractStringField(messageData, "userAvatarUrl", null))
            .build();

        // 메시지 내용 빌드
        ChatMessage.MessageContent messageContent = buildMessageContent(messageType, content, messageData);

        // 클라이언트 정보 빌드
        ChatMessage.ClientInfo clientInfo = ChatMessage.ClientInfo.builder()
            .tempId((String) messageData.get("clientTempId"))
            .platform(extractStringField(messageData, "platform", "WEB"))
            .version(extractStringField(messageData, "version", "1.0"))
            .build();

        // 메시지 상태 빌드
        ChatMessage.MessageStatus status = ChatMessage.MessageStatus.builder()
            .isDeleted(false)
            .isEdited(false)
            .build();

        // 전달 정보 빌드
        ChatMessage.DeliveryInfo delivery = ChatMessage.DeliveryInfo.builder()
            .deliveredTo(List.of(userId)) // 일단 발신자에게만 전달된 것으로 설정
            .readBy(List.of()) // 초기에는 읽지 않음
            .lastDeliveryAttempt(now)
            .build();

        // 메트릭 정보 빌드
        ChatMessage.MessageMetrics metrics = ChatMessage.MessageMetrics.builder()
            .reactions(Map.of())
            .mentions(List.of())
            .hashtags(List.of())
            .build();

        return ChatMessage.builder()
            .messageId(messageId)
            .roomIdentifier(roomId.toString())
            .userIdentifier(userId.toString())
            .userInfo(userInfo)
            .type(messageType)
            .content(messageContent)
            .clientInfo(clientInfo)
            .status(status)
            .delivery(delivery)
            .metrics(metrics)
            .timestamp(timestamp)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    /**
     * 메시지 내용 빌드
     */
    private ChatMessage.MessageContent buildMessageContent(String messageType, String content, Map<String, Object> messageData) {
        ChatMessage.MessageContent.MessageContentBuilder builder = ChatMessage.MessageContent.builder();

        if ("TEXT".equals(messageType) || "SYSTEM".equals(messageType)) {
            builder.text(content);
        }

        // 미디어 타입인 경우 미디어 정보 설정
        if (Set.of("IMAGE", "GIF", "VIDEO", "FILE").contains(messageType)) {
            ChatMessage.MediaInfo mediaInfo = buildMediaInfo(messageData);
            if (mediaInfo != null) {
                builder.media(mediaInfo);
            }
        }

        // 시스템 메시지인 경우 시스템 정보 설정
        if ("SYSTEM".equals(messageType)) {
            ChatMessage.SystemInfo systemInfo = ChatMessage.SystemInfo.builder()
                .action(extractStringField(messageData, "systemAction", "UNKNOWN"))
                .data(Map.of("content", content))
                .build();
            builder.system(systemInfo);
        }

        return builder.build();
    }

    /**
     * 미디어 정보 빌드
     */
    private ChatMessage.MediaInfo buildMediaInfo(Map<String, Object> messageData) {
        String fileUrl = (String) messageData.get("fileUrl");
        if (fileUrl == null) {
            return null;
        }

        ChatMessage.MediaInfo.MediaInfoBuilder builder = ChatMessage.MediaInfo.builder()
            .url(fileUrl)
            .thumbnailUrl((String) messageData.get("thumbnailUrl"))
            .mimeType(extractStringField(messageData, "mimeType", "application/octet-stream"));

        // 파일 크기 파싱
        String fileSizeStr = (String) messageData.get("fileSize");
        if (fileSizeStr != null) {
            try {
                builder.size(Long.parseLong(fileSizeStr));
            } catch (NumberFormatException e) {
                log.warn("[MessageStorage] 파일 크기 파싱 실패: fileSize={}", fileSizeStr);
            }
        }

        // 메타데이터 처리
        String metadata = (String) messageData.get("metadata");
        if (metadata != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadataMap = (Map<String, Object>) messageData.get("metadata");
                builder.metadata(metadataMap != null ? metadataMap : Map.of());
            } catch (Exception e) {
                log.warn("[MessageStorage] 메타데이터 파싱 실패: metadata={}", metadata);
                builder.metadata(Map.of());
            }
        }

        return builder.build();
    }

    /**
     * 타임스탬프 파싱 유틸리티
     */
    private LocalDateTime parseTimestamp(String timestampStr) {
        if (timestampStr == null) {
            return LocalDateTime.now();
        }

        try {
            return LocalDateTime.parse(timestampStr);
        } catch (Exception e) {
            log.warn("[MessageStorage] 타임스탬프 파싱 실패, 현재 시간 사용: timestamp={}", timestampStr);
            return LocalDateTime.now();
        }
    }

    /**
     * 문자열 필드 추출 유틸리티
     */
    private String extractStringField(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 방별 메시지 개수 제한 관리 (선택적 구현)
     * 방당 최대 1000개 메시지 유지
     */
    private void manageRoomMessageLimit(String roomIdentifier) {
        try {
            long messageCount = chatMessageRepository.countByRoomIdentifierAndStatusIsDeletedFalse(roomIdentifier);
            int messageLimit = 1000;

            if (messageCount > messageLimit) {
                // MongoDB에서는 직접 삭제 대신 상태를 변경하여 논리 삭제 처리
                int excessCount = (int) (messageCount - messageLimit + 100); // 여유분

                log.info("[MessageStorage] 방 메시지 제한 초과: roomId={}, current={}, limit={}, excess={}",
                    roomIdentifier, messageCount, messageLimit, excessCount);

                // 실제 삭제는 별도의 배치 작업으로 처리하는 것을 권장
                // 여기서는 로그만 남기고 나중에 아카이빙 작업에서 처리
            }

        } catch (Exception e) {
            log.error("[MessageStorage] 방 메시지 제한 관리 실패: roomId={}", roomIdentifier, e);
            // 메시지 저장은 성공했으므로 예외를 던지지 않음
        }
    }

    /**
     * 스토리지 헬스체크
     */
    @Override
    public boolean isHealthy() {
        try {
            // 간단한 MongoDB 연결 확인
            chatMessageRepository.count();
            return true;
        } catch (Exception e) {
            log.error("[MessageStorage] 헬스체크 실패: MongoDB 연결 불가", e);
            return false;
        }
    }

    /**
     * 통계 정보 제공
     */
    public long getTotalMessageCount() {
        try {
            return chatMessageRepository.count();
        } catch (Exception e) {
            log.error("[MessageStorage] 총 메시지 수 조회 실패", e);
            return -1;
        }
    }

    public long getRoomMessageCount(String roomIdentifier) {
        try {
            return chatMessageRepository.countByRoomIdentifierAndStatusIsDeletedFalse(roomIdentifier);
        } catch (Exception e) {
            log.error("[MessageStorage] 방 메시지 수 조회 실패: roomId={}", roomIdentifier, e);
            return -1;
        }
    }

    /**
     * 클라이언트 임시 ID로 메시지 조회 (ACK 처리용)
     */
    public ChatMessage findByClientTempId(String tempId) {
        try {
            return chatMessageRepository.findByClientInfoTempId(tempId).orElse(null);
        } catch (Exception e) {
            log.error("[MessageStorage] 클라이언트 임시 ID로 메시지 조회 실패: tempId={}", tempId, e);
            return null;
        }
    }
}