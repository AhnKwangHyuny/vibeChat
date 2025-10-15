package com.vibechat.service.enrichment;

import com.vibechat.dto.SendMessagePayload;
import com.vibechat.dto.enrichment.EnrichedMessage;
import com.vibechat.domain.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 메시지 강화 서비스 구현체
 *
 * 메시지에 타임스탬프, 사용자/방 정보, 메타데이터 추가
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageEnricherImpl implements MessageEnricher {

    private final ObjectMapper objectMapper;

    @Override
    @Deprecated
    public EnrichedMessage enrich(SendMessagePayload payload, Long userId, Long roomId) {
        log.warn("Deprecated enrich 호출됨. 사용자 정보 없이 처리. userId={}", userId);
        return enrichWithUserInfo(payload, userId, roomId, null, null);
    }

    @Override
    public EnrichedMessage enrichWithUserInfo(SendMessagePayload payload, Long userId, Long roomId, String nickname, String avatarUrl) {
        try {
            log.debug("Enriching message: type={}, userId={}, roomId={}",
                payload.getType(), userId, roomId);

            MessageType messageType = MessageType.fromString(String.valueOf(payload.getType()));
            LocalDateTime timestamp = LocalDateTime.now();
            String metadata = generateMetadata(payload, timestamp);

            EnrichedMessage enrichedMessage = EnrichedMessage.builder()
                .messageType(messageType)
                .content(payload.getContentText())
                .fileUrl(payload.getMediaUrl())
                .fileName(payload.getFilename())
                .thumbnailUrl(payload.getMediaThumbUrl())
                .userId(userId)
                .nickname(nickname)     // 🆕 닉네임 추가
                .avatarUrl(avatarUrl)   // 🆕 아바타 URL 추가
                .roomId(roomId)
                .timestamp(timestamp)
                .clientTempId(payload.getClientTempId())
                .metadata(metadata)
                .build();

            log.debug("Message enrichment completed: messageType={}, timestamp={}",
                messageType, timestamp);

            return enrichedMessage;

        } catch (Exception e) {
            log.error("Failed to enrich message: payload={}, userId={}, roomId={}",
                payload, userId, roomId, e);
            throw new RuntimeException("Message enrichment failed", e);
        }
    }

    private String generateMetadata(SendMessagePayload payload, LocalDateTime timestamp) {
        try {
            Map<String, Object> metadata = new HashMap<>();

            // 기본 메타데이터
            metadata.put("enrichedAt", timestamp.toString());
            metadata.put("version", "1.0");

            // 메시지 타입별 메타데이터
            MessageType messageType = MessageType.fromString(payload.getType().toString());
            switch (messageType) {
                case IMAGE:
                case GIF:
                case VIDEO:
                    addMediaMetadata(metadata, payload);
                    break;
                case FILE:
                    addFileMetadata(metadata, payload);
                    break;
                case TEXT:
                    addTextMetadata(metadata, payload);
                    break;
                case SYSTEM:
                    addSystemMetadata(metadata, payload);
                    break;
            }

            return objectMapper.writeValueAsString(metadata);

        } catch (Exception e) {
            log.warn("Failed to generate metadata, using minimal metadata", e);
            return "{\"enrichedAt\":\"" + timestamp.toString() + "\",\"version\":\"1.0\"}";
        }
    }

    private void addMediaMetadata(Map<String, Object> metadata, SendMessagePayload payload) {
        metadata.put("mediaType", payload.getType().toString().toLowerCase());
        // fileName is not available in SendMessagePayload
        if (payload.getMediaThumbUrl() != null) {
            metadata.put("hasThumbnail", true);
        }
    }

    private void addFileMetadata(Map<String, Object> metadata, SendMessagePayload payload) {
        // fileName is not available in SendMessagePayload
    }

    private void addTextMetadata(Map<String, Object> metadata, SendMessagePayload payload) {
        if (payload.getContentText() != null) {
            metadata.put("contentLength", payload.getContentText().length());

            // 간단한 텍스트 분석
            boolean hasLinks = payload.getContentText().contains("http");
            boolean hasEmojis = containsEmojis(payload.getContentText());

            metadata.put("hasLinks", hasLinks);
            metadata.put("hasEmojis", hasEmojis);
        }
    }

    private void addSystemMetadata(Map<String, Object> metadata, SendMessagePayload payload) {
        metadata.put("systemMessage", true);

        // 시스템 메시지 타입 추론
        if (payload.getContentText() != null) {
            String content = payload.getContentText().toLowerCase();
            if (content.contains("입장") || content.contains("joined")) {
                metadata.put("systemType", "user_join");
            } else if (content.contains("퇴장") || content.contains("left")) {
                metadata.put("systemType", "user_leave");
            } else {
                metadata.put("systemType", "general");
            }
        }
    }

    private boolean containsEmojis(String text) {
        // 간단한 이모지 감지 (유니코드 범위 체크)
        return text.codePoints().anyMatch(codepoint ->
            (codepoint >= 0x1F600 && codepoint <= 0x1F64F) || // Emoticons
            (codepoint >= 0x1F300 && codepoint <= 0x1F5FF) || // Misc Symbols
            (codepoint >= 0x1F680 && codepoint <= 0x1F6FF) || // Transport
            (codepoint >= 0x2600 && codepoint <= 0x26FF)      // Misc symbols
        );
    }
}
