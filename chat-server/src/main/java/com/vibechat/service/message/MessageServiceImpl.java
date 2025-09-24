package com.vibechat.service.message;

import com.vibechat.dto.SendMessagePayload;
import com.vibechat.dto.UserSummaryDto;
import com.vibechat.dto.WebSocketMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl implements MessageService {

    private final SimpMessagingTemplate messagingTemplate;

    private static final PolicyFactory SANITIZER_POLICY = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    @Override
    public void broadcastMessage(Long roomId, String nickname, String avatarUrl, SendMessagePayload payload) {
        try {
            log.debug("Broadcasting message for room: {}, user: {}, type: {}", roomId, nickname, payload.getType());

            // 입력 검증
            if ("TEXT".equals(payload.getType())) {
                if (payload.getContentText() == null || payload.getContentText().trim().isEmpty()) {
                    throw new IllegalArgumentException("Text message cannot be empty");
                }
                if (payload.getContentText().length() > 2000) {
                    throw new IllegalArgumentException("Message too long (max 2000 characters)");
                }
            }

            // 콘텐츠 sanitization
            String sanitizedContent = payload.getContentText();
            if (sanitizedContent != null) {
                sanitizedContent = SANITIZER_POLICY.sanitize(sanitizedContent);
                if (!sanitizedContent.equals(payload.getContentText())) {
                    log.warn("Message content was sanitized for user: {}", nickname);
                }
            }

            // WebSocket 응답 생성 (DB 저장 없이 바로 브로드캐스트)
            WebSocketMessageResponse response = WebSocketMessageResponse.builder()
                .id(System.currentTimeMillis()) // 임시 ID (나중에 Redis Streams로 대체)
                .clientTempId(payload.getClientTempId())
                .roomId(roomId)
                .user(UserSummaryDto.builder()
                    .nickname(nickname)
                    .avatarUrl(avatarUrl)
                    .build())
                .type(payload.getType())
                .contentText(sanitizedContent)
                .mediaUrl(payload.getMediaUrl())
                .mediaThumbUrl(payload.getMediaThumbUrl())
                .mediaDurationSec(payload.getDurationSec())
                .createdAt(LocalDateTime.now())
                .build();

            // 실시간 브로드캐스트
            messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/messages", response);
            log.info("Message broadcasted successfully for room: {}, user: {}", roomId, nickname);
        } catch (Exception e) {
            log.error("Error broadcasting message for room: {}, user: {}", roomId, nickname, e);
            throw e;
        }
    }


}


