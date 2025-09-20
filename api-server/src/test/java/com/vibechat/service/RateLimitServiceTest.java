package com.vibechat.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RateLimitServiceTest {

    @Test
    void 동일사용자_동일방_21회중_마지막차단() {
        // given
        RateLimitService svc = new RateLimitService();
        long userId = 1L; long roomId = 1L;

        // when
        boolean allowed20 = true;
        for (int i = 0; i < 20; i++) {
            allowed20 &= svc.tryConsume(userId, roomId);
        }
        boolean allowed21 = svc.tryConsume(userId, roomId);

        // then
        assertThat(allowed20).isTrue();
        assertThat(allowed21).isFalse();
        assertThat(svc.nanosToWait(userId, roomId)).isGreaterThan(0);
    }
}


