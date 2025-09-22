# 🚀 VibeChat - 개선된 마이크로서비스 아키텍처

## 📊 시스템 개요

### **아키텍처 핵심 원칙**
- **명확한 책임 분리**: API 서버(비즈니스 로직 + 파일 처리), Chat 서버(실시간 통신)
- **메시지 타입별 최적화**: 텍스트와 미디어 메시지 다른 플로우
- **하이브리드 메시지 큐**: Redis Streams + Pub/Sub + Event Sourcing
- **무손실 + 고성능**: 영구성과 실시간성 동시 보장

---

## 🏗️ 서비스 분리 구조

```
🌐 프론트엔드 (React)
├─ WebSocket 연결: Chat-Server
├─ REST API 호출: API-Server
└─ 파일 업로드: API-Server

🔄 Nginx 리버스 프록시
├─ /api/* → API-Server (Spring Boot)
├─ /ws/* → Chat-Server (Spring Boot + WebSocket)
└─ /uploads/* → Static Files

🏢 API-Server (Spring Boot 3.3.4, Java 21) - 포트 8080
├─ REST API (/api/* endpoints)
├─ 비즈니스 로직 (인증, 방 관리, 사용자 관리)
├─ 파일 업로드 & S3 통합
├─ 메시지 조회 API (페이지네이션)
├─ Redis Event Publisher
├─ MySQL 데이터 관리 (JPA/Hibernate)
└─ 의존성: Spring Web, JPA, Security, AWS SDK

🔥 Chat-Server (Spring Boot 3.3.4, Java 21) - 포트 8081
├─ WebSocket/STOMP 연결 관리 (/ws endpoint)
├─ 실시간 메시지 브로드캐스트
├─ Redis Streams Producer (메시지 큐잉)
├─ Redis Event Subscriber (미디어 이벤트)
├─ Presence & Typing 상태 관리
├─ Rate Limiting (Bucket4j + Redis)
└─ 의존성: Spring WebSocket, Redis, 최소 의존성

🚀 Redis Cluster
├─ Streams: 메시지 영구 저장
├─ Pub/Sub: 실시간 이벤트 라우팅
├─ Cache: 세션, 방 멤버십, Presence
└─ Event: 서버간 통신 채널

💾 데이터 계층
├─ MySQL: 구조화된 데이터 (사용자, 방, 메시지 메타)
├─ S3: 미디어 파일 저장
└─ Message Workers: Redis → MySQL 배치 저장
```

---

## 📱 메시지 플로우 설계

### **시나리오 1: 텍스트 메시지**

```
📝 텍스트 메시지 플로우
├─ 1. Client → Chat-Server: WebSocket send_message
├─ 2. Chat-Server: 인증/권한 검증 + Rate limiting
├─ 3. Chat-Server → Redis Streams: 메시지 영구 저장
│   └─ Key: messages:room:{roomId}
│   └─ Data: {userId, type: "TEXT", content, clientTempId, timestamp}
├─ 4. Chat-Server → Redis Pub/Sub: 실시간 배포
│   └─ Channel: queue:user:{memberId} (각 방 멤버별)
├─ 5. Chat-Server → Client: WebSocket 즉시 브로드캐스트
├─ 6. Message Worker: Redis Streams 구독
└─ 7. Message Worker → MySQL: 배치 저장 (50개 단위)
```

### **시나리오 2: 미디어 메시지 (사진/영상/GIF)**

```
🎨 미디어 메시지 플로우
├─ 1. Client → API-Server: HTTP POST /api/upload
├─ 2. API-Server: 파일 검증 (크기, 타입, 바이러스)
├─ 3. API-Server → S3: 파일 업로드 + 썸네일 생성
├─ 4. API-Server → MySQL: 메타데이터 저장
│   └─ Table: messages {id, roomId, userId, type, mediaUrl, thumbUrl}
├─ 5. API-Server → Redis Event: 미디어 메시지 이벤트 발행
│   └─ Channel: media:message:event
│   └─ Data: {messageId, roomId, userId, type, mediaUrl, thumbUrl}
├─ 6. Chat-Server: Redis Event 구독 → 이벤트 수신
├─ 7. Chat-Server → Redis Streams: 메시지 저장 (영구성)
├─ 8. Chat-Server → Redis Pub/Sub: 실시간 배포
└─ 9. Chat-Server → Client: WebSocket 브로드캐스트
```

---

## 🔄 Redis 메시지 큐 아키텍처

### **3-Layer 메시지 처리**

#### **Layer 1: 실시간 이벤트 라우팅**
```
⚡ Redis Pub/Sub (즉시성)
├─ Text Message: Chat-Server → Redis Pub/Sub
├─ Media Message: API-Server → Redis Event → Chat-Server → Redis Pub/Sub
├─ Channel 구조:
│   ├─ queue:user:{userId}: 개별 사용자 메시지 큐
│   ├─ media:message:event: 미디어 메시지 이벤트
│   ├─ presence:room:{roomId}: 접속 상태 브로드캐스트
│   └─ typing:room:{roomId}: 타이핑 상태 브로드캐스트
└─ 온라인 사용자: 즉시 WebSocket 전달
```

#### **Layer 2: 영구 저장 및 순서 보장**
```
💾 Redis Streams (영구성)
├─ Stream Keys:
│   ├─ messages:room:{roomId}: 방별 메시지 스트림
│   ├─ events:media:upload: 미디어 업로드 이벤트
│   └─ events:presence:{roomId}: 접속 이벤트
├─ Consumer Groups:
│   ├─ message-processors: MySQL 저장 워커
│   ├─ analytics-processors: 통계 처리
│   └─ notification-processors: 푸시 알림
└─ 순서 보장: 방별 Sequential 처리
```

#### **Layer 3: 배치 DB 저장**
```
🔄 Message Workers
├─ Worker Process: 독립 실행 프로세스
├─ Batch Size: 50개 메시지 단위
├─ Processing: 200ms 간격 폴링
├─ DB 저장: MySQL messages 테이블
├─ Idempotency: clientTempId 기반 중복 제거
└─ Error Handling: DLQ (Dead Letter Queue)
```

---

## 📡 서버간 통신 명세

### **API-Server → Chat-Server 통신**

#### **1. 미디어 메시지 이벤트**
```json
// Channel: media:message:event
{
  "eventType": "MEDIA_MESSAGE_UPLOADED",
  "messageId": 12345,
  "roomId": 1,
  "userId": 101,
  "userNickname": "앨리스",
  "type": "IMAGE", // IMAGE, VIDEO, GIF
  "mediaUrl": "/uploads/image_abc123.jpg",
  "thumbUrl": "/uploads/thumb_abc123.jpg",
  "mediaDurationSec": null, // VIDEO만 해당
  "clientTempId": "uuid-from-client",
  "timestamp": "2025-09-22T01:00:00Z"
}
```

#### **2. 방 멤버십 변경 이벤트**
```json
// Channel: room:membership:event
{
  "eventType": "USER_JOINED_ROOM", // USER_LEFT_ROOM
  "roomId": 1,
  "userId": 101,
  "userNickname": "앨리스",
  "timestamp": "2025-09-22T01:00:00Z"
}
```

### **Chat-Server → API-Server 통신**

#### **1. 텍스트 메시지 저장 요청 (옵션)**
```json
// Channel: text:message:save
{
  "eventType": "TEXT_MESSAGE_SAVE",
  "tempId": "redis-stream-id",
  "roomId": 1,
  "userId": 101,
  "content": "안녕하세요!",
  "clientTempId": "uuid-from-client",
  "timestamp": "2025-09-22T01:00:00Z"
}
```

---

## 🛠️ 구현 명세

### **API-Server 주요 컴포넌트**

#### **1. 파일 업로드 컨트롤러**
```java
@RestController
@RequestMapping("/api/upload")
public class MediaUploadController {

    @PostMapping("/media")
    public ResponseEntity<MediaUploadResponse> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam("roomId") Long roomId,
            @AuthUser UserPrincipal user) {
        // 1. 파일 검증
        // 2. S3 업로드 + 썸네일 생성
        // 3. MySQL 메타데이터 저장
        // 4. Redis Event 발행
        return ResponseEntity.ok(response);
    }
}
```

#### **2. Redis Event Publisher**
```java
@Service
public class MediaEventPublisher {

    public void publishMediaMessageEvent(Message message) {
        MediaMessageEvent event = MediaMessageEvent.builder()
            .eventType("MEDIA_MESSAGE_UPLOADED")
            .messageId(message.getId())
            .roomId(message.getChatRoom().getId())
            .userId(message.getUser().getId())
            .type(message.getType().name())
            .mediaUrl(message.getMediaUrl())
            .thumbUrl(message.getMediaThumbUrl())
            .build();

        redisTemplate.convertAndSend("media:message:event", event);
    }
}
```

### **Chat-Server 주요 컴포넌트**

#### **1. WebSocket 메시지 핸들러**
```java
@MessageMapping("/rooms/{roomId}/send")
public void handleTextMessage(
        @DestinationVariable Long roomId,
        SendMessagePayload payload,
        StompHeaderAccessor headers) {
    // 1. 인증/권한 검증
    // 2. Rate limiting
    // 3. Redis Streams 저장
    // 4. Redis Pub/Sub 브로드캐스트
}
```

#### **2. Redis Event Subscriber (Chat-Server)**
```java
@Service
@RequiredArgsConstructor
public class MediaEventSubscriber {

    private final MessageStreamProducer messageProducer;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    @RedisEventListener(channels = "media:message:event")
    public void handleMediaMessageEvent(MediaMessageEvent event) {
        // 1. Redis Streams에 저장 (영구성)
        messageProducer.publishMediaMessage(event);

        // 2. WebSocket 브로드캐스트 (실시간)
        WebSocketMessageResponse response = WebSocketMessageResponse.builder()
            .id(event.getMessageId())
            .roomId(event.getRoomId())
            .type(event.getType())
            .mediaUrl(event.getMediaUrl())
            .thumbUrl(event.getThumbUrl())
            .build();

        messagingTemplate.convertAndSend(
            "/topic/rooms/" + event.getRoomId() + "/messages", response);
    }
}
```

---

## 📋 구현 로드맵

### **Phase 1: 기반 분리 (✅ 완료됨)**
1. **API-Server WebSocket 코드 제거** ✅
   - PresenceService의 STOMP 관련 메서드 제거됨
   - WebSocket 의존성 정리 완료
   - 순수 REST API 서버로 구성

2. **Chat-Server REST API 코드 제거** ✅
   - MessageController REST API 제거 완료
   - 비즈니스 로직 의존성 정리 완료
   - WebSocket + Redis 전용 구조로 정리

3. **의존성 최적화** ✅
   - API-Server: Spring Web, JPA, Security, Redis, AWS SDK
   - Chat-Server: Spring WebSocket, Redis, 최소 의존성
   - pom.xml 정리 완료

### **Phase 2: Redis Streams 메시지 큐 구축 (🔄 진행 중)**
1. **Redis Streams Configuration** (다음 단계)
   - StreamMessageListenerContainer 설정
   - Consumer Group 관리 로직
   - Connection Pool 최적화

2. **Message Producer/Consumer** (다음 단계)
   - MessageStreamProducer 구현 (Chat-Server)
   - MessageStreamConsumer 구현 (API-Server)
   - Dead Letter Queue 처리

3. **Redis Event 시스템**
   - MediaEventPublisher 구현 (API-Server)
   - MediaEventSubscriber 구현 (Chat-Server)
   - Pub/Sub 채널 연동

### **Phase 3: S3 미디어 처리 구현 (🔄 계획)**
1. **S3 업로드 시스템**
   - AWS SDK 통합
   - S3FileUploadService 구현
   - 썸네일 생성 자동화

2. **미디어 메시지 파이프라인**
   - 파일 업로드 → 이벤트 발행 → 실시간 브로드캐스트
   - CDN 최적화 (CloudFront)
   - 미디어 타입별 처리

### **Phase 4: 성능 최적화 & 테스트 (🔄 계획)**
1. **성능 최적화**
   - Virtual Scrolling (프론트엔드)
   - Database 인덱싱 최적화
   - Redis Connection Pool 튜닝

2. **테스트 & 모니터링**
   - 1,000명 동시 접속 부하 테스트
   - 메시지 지연 시간 모니터링 (p95 < 1초)
   - 실시간 성능 대시보드

---

## 🎯 다음 구현 우선순위

**다음 즉시 시작할 작업 (우선순위 순):**

### **🔴 Phase 2-1: Redis Streams 기반 메시지 큐 구현 (3-4일)**
```java
// 1. RedisStreamsConfig.java 설정
// 2. MessageStreamProducer.java 구현 (Chat-Server)
// 3. MessageStreamConsumer.java 구현 (API-Server)
// 4. WebSocket 메시지 핸들러 Redis Streams 연동
```

### **🟡 Phase 2-2: 프론트엔드 연동 복원 (1-2일)**
```typescript
// 1. useMessages.ts 주석 해제 및 수정
// 2. API 엔드포인트 수정 (/api/messages/{roomId})
// 3. WebSocket 연결 테스트
// 4. 실시간 메시지 송수신 확인
```

### **🟢 Phase 2-3: 통합 테스트 및 디버깅 (1일)**
```bash
# 1. 두 브라우저 동시 접속 테스트
# 2. 메시지 전송 → Redis Streams → MySQL 저장 플로우 확인
# 3. 성능 측정 (메시지 지연 시간)
# 4. 에러 상황 처리 테스트
```

### **📋 세부 구현 가이드:**
- **IMPLEMENTATION_TODOLIST.md**: 70일 세부 계획 (10 Phases)
- **TRD_VIBECHAT_MESSAGE_SYSTEM.md**: 완전한 코드 예제
- **PRD_VIBECHAT_MESSAGE_SYSTEM.md**: 요구사항 및 성공 지표

**🚀 이제 TASK-001 (Redis Streams Configuration)부터 시작하여 단계별로 구현하시면 됩니다!**