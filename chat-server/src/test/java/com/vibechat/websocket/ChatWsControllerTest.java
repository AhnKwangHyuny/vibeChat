package com.vibechat.websocket;

/**
 * @deprecated 이 테스트는 구 아키텍처 기반입니다.
 *
 * 새로운 테스트:
 * - ChatMessageControllerTest: 메시지 전송 테스트
 * - RoomControllerTest: 방 입장/퇴장 테스트
 *
 * TODO: 이 파일을 제거하고 새로운 테스트로 마이그레이션
 */
@Deprecated
class ChatWsControllerTest {

//    @Test
//    void 사용자세션없음_send_호출시_UNAUTHORIZED_JSON_큐전송() {
//        // given
//        MessageService messageService = mock(MessageService.class);
//        PresenceService presenceService = mock(PresenceService.class);
//        RateLimitService rateLimitService = mock(RateLimitService.class);
//        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
//        ChatMessageController ctrl = new ChatMessageController(messageService, presenceService, rateLimitService, template);
//
//        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
//        accessor.setSessionId("sess-1");
//        // sessionAttributes 없음 => unauthorized 시나리오
//
//        SendMessagePayload payload = new SendMessagePayload();
//        payload.setClientTempId("tmp");
//        payload.setType(com.vibechat.domain.Message.MessageType.TEXT);
//        payload.setContentText("hi");
//
//        // when
//        ctrl.sendMessage(1L, payload, accessor);
//
//        // then
//        verify(template, times(1)).convertAndSendToUser(eq("sess-1"), eq("/queue/errors"), anyMap());
//        verifyNoInteractions(messageService);
//    }
//
//    @Test
//    void 레이트리밋초과시_RATE_LIMIT_JSON_큐전송() {
//        // given
//        MessageService messageService = mock(MessageService.class);
//        PresenceService presenceService = mock(PresenceService.class);
//        RateLimitService rateLimitService = mock(RateLimitService.class);
//        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
//        ChatWsController ctrl = new ChatWsController(messageService, presenceService, rateLimitService, template);
//
//        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
//        accessor.setSessionId("sess-2");
//        Map<String, Object> sessionAttrs = new HashMap<>();
//        sessionAttrs.put("userId", 10L);
//        accessor.setSessionAttributes(sessionAttrs);
//
//        when(rateLimitService.tryConsume(10L, 5L)).thenReturn(false);
//        when(rateLimitService.nanosToWait(10L, 5L)).thenReturn(3_000_000_000L);
//
//        SendMessagePayload payload = new SendMessagePayload();
//        payload.setClientTempId("tmp");
//        payload.setType(com.vibechat.domain.Message.MessageType.TEXT);
//        payload.setContentText("hi");
//
//        // when
//        ctrl.sendMessage(5L, payload, accessor);
//
//        // then
//        verify(template, times(1)).convertAndSendToUser(eq("sess-2"), eq("/queue/errors"), argThat(map -> ((Map<?,?>) map).get("type").equals("RATE_LIMIT")));
//        verifyNoInteractions(messageService);
//    }
}

