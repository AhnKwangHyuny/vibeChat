package com.vibechat.dto.enrichment;

import com.vibechat.domain.MessageType;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 강화된 메시지 DTO
 *
 * Redis Streams에 발행되는 완전한 메시지 데이터
 */
@Getter
@Builder
@ToString
public class EnrichedMessage {

    private final MessageType messageType;
    private final String content;
    private final String fileUrl;
    private final String fileName;
    private final String thumbnailUrl;
    private final Long userId;
    private final String nickname;
    private final String avatarUrl;
    private final Long roomId;
    private final LocalDateTime timestamp;
    private final String clientTempId;
    private final String metadata;
}