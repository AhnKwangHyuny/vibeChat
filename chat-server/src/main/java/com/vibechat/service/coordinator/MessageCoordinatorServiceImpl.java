package com.vibechat.service.coordinator;

import com.vibechat.dto.SendMessagePayload;
import com.vibechat.dto.coordinator.MessageProcessResult;
import com.vibechat.dto.validation.ValidationResult;
import com.vibechat.dto.enrichment.EnrichedMessage;
import com.vibechat.service.RateLimitService;
import com.vibechat.service.validation.MessageValidator;
import com.vibechat.service.enrichment.MessageEnricher;
import com.vibechat.service.streams.RedisStreamsProducer;
import com.vibechat.exception.MessageValidationException;
import com.vibechat.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메시지 처리 조정 서비스 구현체
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageCoordinatorServiceImpl implements MessageCoordinatorService {

    private final RateLimitService rateLimitService;
    private final MessageValidator messageValidator;
    private final MessageEnricher messageEnricher;
    private final RedisStreamsProducer redisStreamsProducer;

    @Override
    @Transactional
    public MessageProcessResult processRoomMessage(Long roomId, Long userId, SendMessagePayload payload) {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("메시지 프로세스 시작: roomId={}, userId={}, type={}",
                roomId, userId, payload.getType());

            // 1. 레이트 리미팅 체크 (가장 먼저, 빠른 실패)
            rateLimitService.checkRateLimit(userId, roomId);

            // 2. 메시지 검증 (비즈니스 룰 검증)
            ValidationResult validation = messageValidator.validate(payload);
            if (!validation.isValid()) {
                String errors = String.join(", ", validation.getErrors());
                throw new MessageValidationException("[Message Coordinator 메시지 검증 실패: " + errors);
            }

            // 3. 메시지 강화 (메타데이터 추가)
            EnrichedMessage enrichedMessage = messageEnricher.enrich(payload, userId, roomId);

            // 4. Redis Streams 발행 (비동기 처리 시작점)
            String messageId = redisStreamsProducer.sendToRoom(roomId, enrichedMessage);

            // 5. 성공 결과 반환
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("메시지 프로세스 완료: messageId={}, processingTime={}ms",
                messageId, processingTime);

            return MessageProcessResult.builder()
                .success(true)
                .messageId(messageId)
                .clientTempId(payload.getClientTempId())
                .processingTimeMs(processingTime)
                .build();

        } catch (RateLimitExceededException e) {
            log.warn("Rate limit exceeded: userId={}, roomId={}", userId, roomId, e);
            return MessageProcessResult.failure("Rate limit exceeded. Please try again later.");

        } catch (MessageValidationException e) {
            log.warn("Message validation failed: userId={}, roomId={}", userId, roomId, e);
            return MessageProcessResult.failure(e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error during message processing: userId={}, roomId={}",
                userId, roomId, e);
            return MessageProcessResult.failure("Internal server error. Please try again.");
        }
    }
}