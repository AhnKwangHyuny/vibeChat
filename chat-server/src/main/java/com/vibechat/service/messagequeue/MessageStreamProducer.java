package com.vibechat.service.messagequeue;

import com.vibechat.dto.StreamMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis Streams에 메시지를 발행(Publish)하는 책임을 가지는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageStreamProducer {

    private final RedisTemplate<String, Object> redisStreamsTemplate;

    private String getStreamKey(Long roomId) {
        return "room:" + roomId + ":messages";
    }

    /**
     * 주어진 DTO를 사용하여 Redis Stream에 메시지를 발행합니다.
     * @param messageDto 발행할 메시지 데이터
     * @return 생성된 레코드의 ID
     */
    public RecordId publishMessage(StreamMessageDto messageDto) {
        String streamKey = getStreamKey(messageDto.getRoomId());
        try {
            // RedisTemplate이 DTO를 JSON으로 변환하여 스트림에 추가합니다.
            ObjectRecord<String, StreamMessageDto> record = ObjectRecord.create(streamKey, messageDto);
            RecordId recordId = redisStreamsTemplate.opsForStream().add(record);

            if (recordId == null) {
                throw new RuntimeException("Failed to get RecordId from Redis.");
            }

            log.info("Published message to stream {}. Message ID: {}", streamKey, recordId.getValue());
            return recordId;
        } catch (Exception e) {
            log.error("Failed to publish message to stream {}: {}", streamKey, e.getMessage(), e);
            // 실무에서는 재시도 로직이나 DLQ 발행 등의 처리가 필요할 수 있습니다.
            throw new RuntimeException("Failed to publish message to stream", e);
        }
    }
}
