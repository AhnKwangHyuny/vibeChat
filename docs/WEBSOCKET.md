# WEBSOCKET.md: 바이브 챗 WebSocket (STOMP) 명세

이 문서는 기술 요구사항 문서(TRD)를 기반으로 바이브 챗 애플리케이션의 WebSocket 통신 프로토콜을 상세히 설명합니다.

## 공통

- **엔드포인트:** `/ws` (SockJS 활성화)
- **STOMP 브로커:** 애플리케이션 목적지 접두사 `/app`을 사용하는 SimpleBroker (인메모리)
- **하트비트:** 클라이언트 ↔ 서버 10000/10000 ms

## 구독 (클라이언트가 메시지를 수신하기 위해 구독)

- **메시지:**
  - **목적지:** `/topic/rooms/{roomId}/messages`
  - **설명:** 특정 채팅방에 대한 새로운 채팅 메시지를 수신합니다.
  - **페이로드 (MessageEvent):**
    ```json
    {
      "id": 101,
      "clientTempId": "uuid-string-from-client", // 선택 사항, 보류 중인 메시지 확인용
      "roomId": 1,
      "user": {
        "id": 1,
        "nickname": "앨리스",
        "avatarUrl": null
      },
      "type": "TEXT" | "IMAGE" | "GIF" | "VIDEO",
      "contentText": "안녕하세요 여러분!", // 타입이 TEXT인 경우 존재
      "mediaUrl": "/uploads/image.jpg", // 타입이 IMAGE/GIF/VIDEO인 경우 존재
      "mediaThumbUrl": "/uploads/thumb.jpg", // 선택 사항, IMAGE/GIF/VIDEO용
      "mediaDurationSec": 10, // 선택 사항, VIDEO용
      "createdAt": "2025-09-09T10:00:00"
    }
    ```

- **입력 중 표시기:**
  - **목적지:** `/topic/rooms/{roomId}/typing`
  - **설명:** 채팅방에서 현재 입력 중인 사용자에 대한 업데이트를 수신합니다.
  - **페이로드 (TypingEvent):**
    ```json
    {
      "roomId": 1,
      "userId": 1,
      "nickname": "앨리스",
      "typing": true, // 입력 중이면 true, 중지되면 false
      "ts": 1678886400000 // 타임스탬프
    }
    ```

- **온라인 상태 (온라인 사용자):**
  - **목적지:** `/topic/rooms/{roomId}/presence`
  - **설명:** 채팅방의 온라인 사용자 수에 대한 업데이트를 수신합니다.
  - **페이로드 (PresenceEvent):**
    ```json
    {
      "roomId": 1,
      "count": 5 // 온라인 사용자 수
    }
    ```

## 메시지 전송 (클라이언트가 메시지를 보내기 위해 발행)

- **메시지 전송:**
  - **목적지:** `/app/rooms/{roomId}/send`
  - **설명:** 특정 채팅방에 새 메시지를 보냅니다.
  - **페이로드 (SendMessagePayload):**
    ```json
    {
      "clientTempId": "uuid-string-from-client", // 보류 중인 메시지 추적을 위해 클라이언트가 생성한 고유 ID
      "type": "TEXT" | "IMAGE" | "GIF" | "VIDEO",
      "contentText": "string (선택 사항)",
      "mediaUrl": "string (선택 사항)",
      "mediaThumbUrl": "string (선택 사항)",
      "mediaDurationSec": "number (선택 사항)"
    }
    ```

- **입력 상태:**
  - **목적지:** `/app/rooms/{roomId}/typing`
  - **설명:** 사용자의 입력 상태를 서버에 알립니다.
  - **페이로드:**
    ```json
    {
      "userId": 1,
      "typing": true // 입력 중이면 true, 중지되면 false
    }
    ```

## ACK/서비스 품질

- **클라이언트 생성 `clientTempId`:** 클라이언트는 `SendMessagePayload`에 고유한 `clientTempId`를 포함해야 합니다. 서버는 성공적인 영속화 후 `MessageEvent` 브로드캐스트에서 이 `clientTempId`를 다시 에코합니다. 이를 통해 클라이언트는 보류 중인 메시지를 확인된 상태로 전환할 수 있습니다.
- **오류 처리:** 메시지 전송 실패 시 서버는 `ERROR` 프레임을 보내거나 REST API가 오류를 반환할 수 있습니다. 클라이언트는 이러한 실패를 처리해야 합니다(예: 보류 중인 메시지 제거, 실패로 표시, 재시도 제안).

## 재연결 전략

- **지수 백오프:** 연결 해제 시 클라이언트는 지수 백오프 전략(예: 1초 → 2초 → 4초, 최대 10초)을 사용하여 재연결을 시도해야 합니다.
- **메시지 큐잉:** 오프라인 상태에서 메시지 전송 요청은 큐에 저장되고 성공적으로 재연결되면 플러시되어야 합니다.
- **구독 재설정:** 성공적으로 재연결되면 모든 활성 구독이 재설정되어야 합니다.