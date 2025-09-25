package com.vibechat.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.time.Instant;

/**
 * Chat-Server 전용 예외 처리기
 *
 * 실시간 메시징 관련 예외만 처리
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MessageValidationException.class)
    public ResponseEntity<ProblemDetail> handleMessageValidation(MessageValidationException ex, HttpServletRequest request) {
        log.warn("메시지 검증 실패 at {}: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("메시지 검증 실패");
        problemDetail.setType(URI.create("https://vibechat.com/problems/message-validation"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("path", request.getRequestURI());
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(RateLimitExceededException ex, HttpServletRequest request) {
        log.warn("메시지 전송 제한 초과 at {}: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problemDetail.setTitle("메시지 전송 제한 초과");
        problemDetail.setType(URI.create("https://vibechat.com/problems/rate-limit"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("path", request.getRequestURI());
        problemDetail.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(problemDetail);
    }

    @ExceptionHandler(NicknameConflictException.class)
    public ResponseEntity<ProblemDetail> handleNicknameConflict(NicknameConflictException ex, HttpServletRequest request) {
        log.warn("닉네임 충돌 at {}: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("닉네임 충돌 발생");
        problemDetail.setType(URI.create("https://vibechat.com/problems/nickname-conflict"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("path", request.getRequestURI());
        problemDetail.setProperty("suggestedNickname", ex.getSuggestedNickname());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("잘못된 인자 오류: {} at {}", ex.getMessage(), request.getRequestURI());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("잘못된 요청 인자");
        problemDetail.setType(URI.create("https://vibechat.com/problems/invalid-argument"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("path", request.getRequestURI());
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("유효성 검증 오류 at {}: {} 개의 검증 오류", request.getRequestURI(), ex.getBindingResult().getErrorCount());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "요청 데이터 유효성 검증에 실패했습니다");
        problemDetail.setTitle("유효성 검증 실패");
        problemDetail.setType(URI.create("https://vibechat.com/problems/validation-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("path", request.getRequestURI());

        // 유효성 검증 오류 추가
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            problemDetail.setProperty(error.getField(), error.getDefaultMessage());
        });

        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("접근 거부 at {}: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "접근이 거부되었습니다");
        problemDetail.setTitle("접근 거부");
        problemDetail.setType(URI.create("https://vibechat.com/problems/access-denied"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ProblemDetail> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        log.error("예상치 못한 오류 at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 예상치 못한 오류가 발생했습니다");
        problemDetail.setTitle("서버 내부 오류");
        problemDetail.setType(URI.create("https://vibechat.com/problems/internal-server-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("path", request.getRequestURI());
        return ResponseEntity.internalServerError().body(problemDetail);
    }
}