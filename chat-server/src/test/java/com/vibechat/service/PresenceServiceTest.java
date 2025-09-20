package com.vibechat.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class PresenceServiceTest {

//    @Test
//    void 구독_연결시_세션해시_TTL_설정_프레즌스_브로드캐스트() {
//        // given
//        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
//        SetOperations<String, String> setOps = mock(SetOperations.class);
//        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
//        when(redis.opsForSet()).thenReturn(setOps);
//        when(redis.opsForHash()).thenReturn(hashOps);
//        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
//        PresenceService svc = new PresenceService(redis, template);
//
//        // when
//        svc.userConnected("sess-1", 1L, 2L, "nick");
//
//        // then
//        verify(hashOps, times(1)).putAll(eq("presence:session:sess-1"), any(Map.class));
//        verify(redis, times(1)).expire(eq("presence:session:sess-1"), any());
//        verify(setOps, times(1)).add(eq("presence:room:2:sessions"), eq("sess-1"));
//        verify(template, atLeastOnce()).convertAndSend(eq("/topic/rooms/2/presence"), anyMap());
//    }
//
//    @Test
//    void 연결해제시_세션삭제_타이핑닉네임_정리() {
//        // given
//        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
//        SetOperations<String, String> setOps = mock(SetOperations.class);
//        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
//        when(redis.opsForSet()).thenReturn(setOps);
//        when(redis.opsForHash()).thenReturn(hashOps);
//        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
//        PresenceService svc = new PresenceService(redis, template);
//
//        Map<Object, Object> sessionInfo = new HashMap<>();
//        sessionInfo.put("roomId", "2");
//        sessionInfo.put("userId", "1");
//        sessionInfo.put("nickname", "nick");
//        when(hashOps.entries("presence:session:sess-1")).thenReturn(sessionInfo);
//
//        // when
//        svc.userDisconnected("sess-1");
//
//        // then
//        verify(setOps, times(1)).remove("presence:room:2:sessions", "sess-1");
//        verify(setOps, atLeastOnce()).remove(eq("typing:room:2"), anyString());
//        verify(setOps, atLeastOnce()).remove("nickname:room:2", "nick");
//        verify(redis, times(1)).delete("presence:session:sess-1");
//        verify(template, atLeastOnce()).convertAndSend(eq("/topic/rooms/2/presence"), anyMap());
//    }
}


