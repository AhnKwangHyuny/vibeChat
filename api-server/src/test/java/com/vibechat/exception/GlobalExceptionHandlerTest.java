package com.vibechat.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    @Test
    void 레이트리밋_예외_429와_retryAfterSeconds_포함() {
        // given
        GlobalExceptionHandler h = new GlobalExceptionHandler();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/test");
        RateLimitExceededException ex = new RateLimitExceededException("too many", 7);

        // when
        ResponseEntity<ProblemDetail> res = h.handleRateLimitExceeded(ex, req);

        // then
        assertThat(res.getStatusCode().value()).isEqualTo(429);
        assertThat(res.getBody().getProperties()).containsEntry("retryAfterSeconds", 7L);
    }

    @Test
    void 닉네임충돌_예외_409와_제안닉네임_포함() {
        // given
        GlobalExceptionHandler h = new GlobalExceptionHandler();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/join");
        NicknameConflictException ex = new NicknameConflictException("conflict", "nick007");

        // when
        ResponseEntity<ProblemDetail> res = h.handleNicknameConflict(ex, req);

        // then
        assertThat(res.getStatusCode().value()).isEqualTo(409);
        assertThat(res.getBody().getProperties()).containsEntry("suggestedNickname", "nick007");
    }
}


