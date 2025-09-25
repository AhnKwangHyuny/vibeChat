package com.vibechat.service.streams;

import com.vibechat.dto.enrichment.EnrichedMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis Streams 메시지 발행 서비스 구현체
 *
 * Redis Streams에 메시지를 발행하여 비동기 처리 시작
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisStreamsProducerImpl implements RedisStreamsProducer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis Streams 키 패턴
    private static final String ROOM_STREAM_KEY = "stream:room:{}";
    private static final String USER_STREAM_KEY = "stream:user:{}";
    private static final String NOTIFICATION_QUEUE_KEY = "stream:notifications";

    @Override
    public String sendToRoom(Long roomId, EnrichedMessage message) {
        try {
            String streamKey = ROOM_STREAM_KEY.replace("{}", roomId.toString());
            Map<String, Object> messageData = buildMessageData(message);

            log.debug("Publishing message to room stream: roomId={}, streamKey={}", roomId, streamKey);

            ObjectRecord<String, Map<String, Object>> record = StreamRecords
                .newRecord()
                .ofObject(messageData)
                .withStreamKey(streamKey);

            RecordId recordId = redisTemplate.opsForStream().add(record);
            String messageId = recordId.getValue();

            log.info("Message published to room stream: roomId={}, messageId={}, type={}",
                roomId, messageId, message.getMessageType());

            return messageId;

        } catch (Exception e) {
            log.error("Failed to publish message to room stream: roomId={}, message={}",
                roomId, message, e);
            throw new RuntimeException("Failed to publish to room stream", e);
        }
    }

    @Override
    public String sendToUser(Long userId, EnrichedMessage message) {
        try {
            String streamKey = USER_STREAM_KEY.replace("{}", userId.toString());
            Map<String, Object> messageData = buildMessageData(message);
            messageData.put("eventType", "MESSAGE_RECEIVED");

            log.debug("Publishing message to user stream: userId={}, streamKey={}", userId, streamKey);

            ObjectRecord<String, Map<String, Object>> record = StreamRecords
                .newRecord()
                .ofObject(messageData)
                .withStreamKey(streamKey);

            RecordId recordId = redisTemplate.opsForStream().add(record);
            String messageId = recordId.getValue();

            log.debug("Message published to user stream: userId={}, messageId={}", userId, messageId);

            return messageId;

        } catch (Exception e) {
            log.error("Failed to publish message to user stream: userId={}, message={}",
                userId, message, e);
            throw new RuntimeException("Failed to publish to user stream", e);
        }
    }

    @Override
    public String sendToNotificationQueue(EnrichedMessage message) {
        try {
            Map<String, Object> messageData = buildMessageData(message);
            messageData.put("eventType", "OFFLINE_NOTIFICATION");
            messageData.put("priority", "NORMAL");

            log.debug("Publishing message to notification queue: roomId={}, messageType={}",
                message.getRoomId(), message.getMessageType());

            ObjectRecord<String, Map<String, Object>> record = StreamRecords
                .newRecord()
                .ofObject(messageData)
                .withStreamKey(NOTIFICATION_QUEUE_KEY);

            RecordId recordId = redisTemplate.opsForStream().add(record);
            String messageId = recordId.getValue();

            log.debug("Message published to notification queue: messageId={}", messageId);

            return messageId;

        } catch (Exception e) {
            log.error("Failed to publish message to notification queue: message={}", message, e);
            throw new RuntimeException("Failed to publish to notification queue", e);
        }
    }

    private Map<String, Object> buildMessageData(EnrichedMessage message) {
        try {
            Map<String, Object> data = new HashMap<>();

            // 기본 필드
            data.put("messageType", message.getMessageType().name());
            data.put("content", message.getContent());
            data.put("userId", message.getUserId());
            data.put("roomId", message.getRoomId());
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
                data.put("metadata", message.getMetadata());
            }

            log.debug("Built message data with {} fields", data.size());
            return data;

        } catch (Exception e) {
            log.error("Failed to build message data: message={}", message, e);
            throw new RuntimeException("Failed to build message data", e);
        }
    }
}