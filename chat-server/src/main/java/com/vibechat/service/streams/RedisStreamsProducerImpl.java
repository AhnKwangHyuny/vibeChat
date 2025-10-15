package com.vibechat.service.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibechat.dto.enrichment.EnrichedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis Streams 메시지 발행 서비스 구현체
 *
 * Room Stream에만 발행 → Consumer Group이 모든 후속 처리 담당
 * - RoomBroadcastConsumer: 실시간 WebSocket 브로드캐스트
 * - MessageStorageConsumer: MongoDB 영구 저장
 * - NotificationConsumer: 오프라인 사용자 푸시 알림
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisStreamsProducerImpl implements RedisStreamsProducer {

    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Redis Streams 키 패턴
    private static final String ROOM_STREAM_KEY = "stream:room:{}";

    @Override
    public String sendToRoom(Long roomId, EnrichedMessage message) {
        try {
            String streamKey = ROOM_STREAM_KEY.replace("{}", roomId.toString());
            Map<String, Object> messageData = buildMessageData(message);

            log.info("Room Stream에 메시지 발행: roomId={}, streamKey={}", roomId, streamKey);

            MapRecord<String, String, Object> record = StreamRecords
                .newRecord()
                .in(streamKey)
                .ofMap(messageData);

            RecordId recordId = redisTemplate.opsForStream().add(record);
            String messageId = recordId.getValue();

            log.info("Room Stream 발행 완료: roomId={}, messageId={}, type={} → Consumer Group에서 비동기 처리 시작",
                roomId, messageId, message.getMessageType());

            return messageId;

        } catch (Exception e) {
            log.error("Room Stream 발행 실패: roomId={}, message={}", roomId, message, e);
            throw new RuntimeException("Failed to publish to room stream", e);
        }
    }


    private Map<String, Object> buildMessageData(EnrichedMessage message) {
        try {
            Map<String, Object> data = new HashMap<>();

            // 기본 필드
            data.put("eventType", "MESSAGE_RECEIVED");
            data.put("messageType", message.getMessageType().name());
            data.put("content", message.getContent());
            data.put("userId", String.valueOf(message.getUserId()));
            data.put("roomId", String.valueOf(message.getRoomId()));
            data.put("timestamp", message.getTimestamp().toString());
            data.put("clientTempId", message.getClientTempId());

            // 선택적 필드
            if (message.getFileUrl() != null) {
                data.put("fileUrl", message.getFileUrl());
            }
            if (message.getFileName() != null) {
                data.put("fileName", message.getFileName());
            }
            if (message.getThumbnailUrl() != null) {
                data.put("thumbnailUrl", message.getThumbnailUrl());
            }
            if (message.getMetadata() != null) {

                data.put("metadata", objectMapper.writeValueAsString(message.getMetadata()));
            }

            if (message.getNickname() != null) {
                data.put("userNickname", message.getNickname());
            }
            if (message.getAvatarUrl() != null) {
                data.put("userAvatarUrl", message.getAvatarUrl());
            }

            log.info("Built message data with {} fields", data.size());
            log.info("show record before building {}", data.toString());
            return data;

        } catch (Exception e) {
            log.error("Failed to build message data: message={}", message, e);
            throw new RuntimeException("Failed to build message data", e);
        }
    }
}