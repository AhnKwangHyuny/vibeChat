package com.vibechat.consumer.room;

import com.vibechat.consumer.core.AbstractMessageConsumer;
import com.vibechat.consumer.core.ConsumerProcessingException;
import com.vibechat.domain.MessageType;
import com.vibechat.dto.WebSocketMessageResponse;
import com.vibechat.dto.UserSummaryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * 방 브로드캐스트 전용 Consumer
 *
 * 책임: stream:room:* 스트림을 구독하여 실시간 WebSocket 브로드캐스트
 * SRP: WebSocket 전송만 담당
 * OCP: 새로운 브로드캐스트 로직 추가 가능
 */
@Component
@Slf4j
public class RoomBroadcastConsumer extends AbstractMessageConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    private static final Set<String> SUPPORTED_TYPES = Set.of(
        "TEXT", "IMAGE", "GIF", "VIDEO", "FILE", "SYSTEM"
    );

    public RoomBroadcastConsumer(SimpMessagingTemplate messagingTemplate) {
        super("RoomBroadcastConsumer", SUPPORTED_TYPES);
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    protected void processBusinessLogic(MapRecord<String, String, Object> record) throws ConsumerProcessingException {
        String messageId = record.getId().getValue();
        Map<String, Object> messageData = extractMessageData(record);

        try {
            // 1. 필수 필드 검증
            requireField(messageData, "roomId", messageId);
            requireField(messageData, "userId", messageId);
            requireField(messageData, "messageType", messageId);

            // 2. 메시지 데이터 추출
            Long roomId = Long.parseLong(messageData.get("roomId").toString());
            Long userId = Long.parseLong(messageData.get("userId").toString());
            String messageType = (String) messageData.get("messageType");
            String content = (String) messageData.get("content");
            String clientTempId = (String) messageData.get("clientTempId");

            // 3. WebSocket 응답 객체 생성
            WebSocketMessageResponse response = buildWebSocketResponse(
                messageId, messageData, roomId, userId, messageType, content, clientTempId
            );

            // 4. 실시간 브로드캐스트
            String destination = "/topic/rooms/" + roomId + "/messages";
            messagingTemplate.convertAndSend(destination, response);

            log.info("[RoomBroadcast] 방 브로드캐스트 완료: roomId={}, messageId={}, type={}",
                roomId, messageId, messageType);

        } catch (NumberFormatException e) {
            throw new ConsumerProcessingException(getConsumerType(), messageId,
                "잘못된 숫자 형식의 roomId 또는 userId", e, false);
        } catch (Exception e) {
            throw new ConsumerProcessingException(getConsumerType(), messageId,
                "WebSocket 브로드캐스트 실패", e);
        }
    }

    /**
     * WebSocket 응답 객체 구성
     */
    private WebSocketMessageResponse buildWebSocketResponse(
        String messageId, Map<String, Object> messageData,
        Long roomId, Long userId, String messageType,
        String content, String clientTempId) {

        // Redis Streams에서 사용자 정보 추출
        // Producer에서 "userNickname", "userAvatarUrl"로 저장함
        String nickname = (String) messageData.get("userNickname");
        String avatarUrl = (String) messageData.get("userAvatarUrl");

        // Fallback 처리: nickname이 없으면 기본값 사용
        if (nickname == null || nickname.isBlank()) {
            nickname = "사용자" + userId;
            log.warn("[RoomBroadcast] nickname 없음, Fallback 사용: userId={}, messageId={}", 
                userId, messageId);
        }

        return WebSocketMessageResponse.builder()
            .id(Long.parseLong(messageId.split("-")[0])) // 타임스탬프 부분 사용
            .clientTempId(clientTempId)
            .roomId(roomId)
            .user(UserSummaryDto.builder()
                .id(userId)
                .nickname(nickname)      // Redis 데이터 사용
                .avatarUrl(avatarUrl)    // Redis 데이터 사용
                .build())
            .type(MessageType.valueOf(messageType))
            .contentText(content)
            .mediaUrl((String) messageData.get("fileUrl"))
            .mediaThumbUrl((String) messageData.get("thumbnailUrl"))
            .createdAt(parseTimestamp((String) messageData.get("timestamp")))
            .build();
    }

    /**
     * 미디어 지속 시간 추출
     */
//    private Integer extractDuration(Map<String, Object> messageData) {
//        String metadata = (String) messageData.get("metadata");
//        if (metadata != null && metadata.contains("duration")) {
//            try {
//                // 메타데이터에서 duration 파싱 (간단 구현)
//                return null; // TODO: JSON 파싱하여 실제 duration 추출
//            } catch (Exception e) {
//                log.debug("미디어 duration 추출 실패, 기본값 사용: {}", e.getMessage());
//            }
//        }
//        return null;
//    }

    /**
     * 타임스탬프 파싱
     */
    private LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp == null) {
            return LocalDateTime.now();
        }

        try {
            return LocalDateTime.parse(timestamp);
        } catch (Exception e) {
            log.warn("타임스탬프 파싱 실패, 현재 시간 사용: timestamp={}", timestamp);
            return LocalDateTime.now();
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            // WebSocket 연결 상태 확인 (간단한 헬스체크)
            return messagingTemplate != null;
        } catch (Exception e) {
            log.error("RoomBroadcastConsumer 헬스체크 실패", e);
            return false;
        }
    }
}