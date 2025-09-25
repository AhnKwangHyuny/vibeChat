package com.vibechat.service.validation;

import com.vibechat.dto.SendMessagePayload;
import com.vibechat.dto.validation.ValidationResult;
import com.vibechat.domain.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 메시지 검증 서비스 구현체
 *
 * XSS 검증, 길이 제한, 미디어 타입, 필수 필드 검증 담당
 */
@Service
@Slf4j
public class MessageValidatorImpl implements MessageValidator {

    private static final int MAX_TEXT_LENGTH = 1000;
    private static final int MAX_FILENAME_LENGTH = 255;
    private static final PolicyFactory POLICY = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    @Override
    public ValidationResult validate(SendMessagePayload payload) {
        List<String> errors = new ArrayList<>();

        try {
            // 1. 필수 필드 검증
            validateRequiredFields(payload, errors);

            if (!errors.isEmpty()) {
                return ValidationResult.failure(errors);
            }

            // 2. 메시지 타입 검증
            validateMessageType(payload, errors);

            // 3. 컨텐츠 타입별 검증
            validateContentByType(payload, errors);

            // 4. XSS 검증 (텍스트 메시지)
            validateXSS(payload, errors);

            if (errors.isEmpty()) {
                log.debug("Message validation successful: type={}, contentLength={}",
                    payload.getType(), getContentLength(payload));
                return ValidationResult.success();
            }

            log.warn("Message validation failed: errors={}", errors);
            return ValidationResult.failure(errors);

        } catch (Exception e) {
            log.error("Unexpected error during message validation", e);
            return ValidationResult.failure("Internal validation error");
        }
    }

    private void validateRequiredFields(SendMessagePayload payload, List<String> errors) {
        if (payload == null) {
            errors.add("Message payload is required");
            return;
        }

        if (!StringUtils.hasText(payload.getType())) {
            errors.add("Message type is required");
        }

        if (!StringUtils.hasText(payload.getClientTempId())) {
            errors.add("Client temporary ID is required");
        }
    }

    private void validateMessageType(SendMessagePayload payload, List<String> errors) {
        try {
            MessageType.fromString(payload.getType());
        } catch (IllegalArgumentException e) {
            errors.add("Invalid message type: " + payload.getType());
        }
    }

    private void validateContentByType(SendMessagePayload payload, List<String> errors) {
        MessageType messageType;
        try {
            messageType = MessageType.fromString(payload.getType());
        } catch (IllegalArgumentException e) {
            return; // 이미 타입 검증에서 에러 추가됨
        }

        switch (messageType) {
            case TEXT:
                validateTextContent(payload, errors);
                break;
            case IMAGE:
            case GIF:
            case VIDEO:
                validateMediaContent(payload, errors);
                break;
            case FILE:
                validateFileContent(payload, errors);
                break;
            case SYSTEM:
                validateSystemContent(payload, errors);
                break;
        }
    }

    private void validateTextContent(SendMessagePayload payload, List<String> errors) {
        String content = payload.getContent();

        if (!StringUtils.hasText(content)) {
            errors.add("Text content is required for text messages");
            return;
        }

        if (content.length() > MAX_TEXT_LENGTH) {
            errors.add("Text message too long. Maximum " + MAX_TEXT_LENGTH + " characters allowed");
        }
    }

    private void validateMediaContent(SendMessagePayload payload, List<String> errors) {
        if (!StringUtils.hasText(payload.getFileUrl())) {
            errors.add("File URL is required for media messages");
        }

        if (StringUtils.hasText(payload.getFileName()) &&
            payload.getFileName().length() > MAX_FILENAME_LENGTH) {
            errors.add("Filename too long. Maximum " + MAX_FILENAME_LENGTH + " characters allowed");
        }

        // 썸네일 URL 검증 (선택적)
        if (StringUtils.hasText(payload.getThumbnailUrl()) &&
            !isValidUrl(payload.getThumbnailUrl())) {
            errors.add("Invalid thumbnail URL format");
        }
    }

    private void validateFileContent(SendMessagePayload payload, List<String> errors) {
        if (!StringUtils.hasText(payload.getFileUrl())) {
            errors.add("File URL is required for file messages");
        }

        if (!StringUtils.hasText(payload.getFileName())) {
            errors.add("Filename is required for file messages");
        } else if (payload.getFileName().length() > MAX_FILENAME_LENGTH) {
            errors.add("Filename too long. Maximum " + MAX_FILENAME_LENGTH + " characters allowed");
        }
    }

    private void validateSystemContent(SendMessagePayload payload, List<String> errors) {
        if (!StringUtils.hasText(payload.getContent())) {
            errors.add("System message content is required");
        }
    }

    private void validateXSS(SendMessagePayload payload, List<String> errors) {
        if (!StringUtils.hasText(payload.getContent())) {
            return;
        }

        String originalContent = payload.getContent();
        String sanitizedContent = POLICY.sanitize(originalContent);

        if (!originalContent.equals(sanitizedContent)) {
            log.warn("XSS attempt detected: original='{}', sanitized='{}'",
                originalContent, sanitizedContent);
            errors.add("Message contains potentially harmful content");
        }
    }

    private boolean isValidUrl(String url) {
        return StringUtils.hasText(url) &&
               (url.startsWith("http://") || url.startsWith("https://"));
    }

    private int getContentLength(SendMessagePayload payload) {
        return StringUtils.hasText(payload.getContent()) ? payload.getContent().length() : 0;
    }
}