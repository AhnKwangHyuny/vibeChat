# Redis Streams 기반 실시간 채팅 아키텍처 설계서

## 문서 정보
- **작성일**: 2024-09-24
- **작성자**: Backend Tech Lead
- **버전**: 2.0
- **목표**: 카카오톡 수준의 확장 가능한 실시간 채팅 시스템

## 📋 1. 아키텍처 개요

### 핵심 설계 원칙
- **Redis Streams Only**: 메시지 영속성 + 실시간 처리 통합
- **이중 WebSocket 구조**: Room WebSocket + User WebSocket 분리
- **Event-Driven Architecture**: 비동기 메시지 처리
- **Horizontal Scaling**: 무상태 서버 클러스터링

### 시스템 구성요소
```
┌─────────────────────────────────────────────────────────────────┐
│                    Load Balancer                                │
└─────────────────┬───────────────────┬───────────────────────────┘
                  │                   │
    ┌─────────────▼──────────┐  ┌─────▼──────────┐  ┌─────────────▼─────────┐
    │   Chat-Server A        │  │ Chat-Server B  │  │   Chat-Server C       │
    │ ┌─────────┬──────────┐ │  │                │  │                       │
    │ │Room WS  │ User WS  │ │  │                │  │                       │
    │ └─────────┴──────────┘ │  │                │  │                       │
    └─────────────┬──────────┘  └─────┬──────────┘  └───────────┬───────────┘
                  │                   │                         │
                  └─────────────────────┼─────────────────────────┘
                                        │
                  ┌─────────────────────▼─────────────────────┐
                  │            Redis Streams Cluster          │
                  │  ┌─────────────────────────────────────┐  │
                  │  │ Room Streams: room:{id}:messages   │  │
                  │  │ User Streams: user:{id}:events     │  │
                  │  │ Global Stream: system:events       │  │
                  │  └─────────────────────────────────────┘  │
                  └─────────────────────┬─────────────────────┘
                                        │
                  ┌─────────────────────▼─────────────────────┐
                  │              MongoDB Cluster              │
                  │  - Message Collection (Sharded)          │
                  │  - User State Collection                  │
                  │  - Room Metadata Collection               │
                  └───────────────────────────────────────────┘
```

## 🔄 2. 메시지 플로우 상세 설계

### 2.1 메시지 전송 플로우
```
[사용자 메시지 전송]
     ↓
[WebSocketController.sendMessage()]
     ↓
[MessageCoordinatorService.processMessage()]
     ├─→ [MessageValidator] → 검증 (XSS, 길이, 타입)
     ├─→ [MessageEnricher] → 메타데이터 추가 (timestamp, userId, clientTempId)
     └─→ [RedisStreamsProducer.sendToRoom()]
             ↓
[Redis Streams: room:{roomId}:messages]
     ├─→ [RoomBroadcastConsumer] → 실시간 WebSocket 브로드캐스트
     ├─→ [UserNotificationConsumer] → 오프라인 사용자 알림
     ├─→ [StorageConsumer] → MongoDB 영구 저장
     └─→ [AnalyticsConsumer] → 통계 및 모니터링
```

### 2.2 읽지 않은 메시지 카운트 플로우
```
[메시지 저장 완료 이벤트]
     ↓
[UnreadCountService.updateUnreadCount()]
     ├─→ Redis: user:{userId}:unread:{roomId} → INCREMENT
     ├─→ Redis: user:{userId}:lastread:{roomId} → GET (비교용)
     └─→ [GlobalNotificationService.notifyUnreadUpdate()]
             ↓
[Redis Streams: user:{userId}:events]
     ↓
[UserNotificationConsumer] → User WebSocket 브로드캐스트
     ↓
[클라이언트: /topic/user/{userId}/room-updates 수신]
```

## 🚀 3. 이중 WebSocket 아키텍처

### 3.1 Room WebSocket (채팅 전용)
```java
@Controller
public class RoomWebSocketController {

    @MessageMapping("/rooms/{roomId}/send")
    public void sendMessage(@DestinationVariable Long roomId,
                           SendMessagePayload payload,
                           @AuthenticationPrincipal UserPrincipal user) {
        messageCoordinator.processRoomMessage(roomId, user.getId(), payload);
    }

    @SubscribeMapping("/topic/rooms/{roomId}/messages")
    public void subscribeToRoom(@DestinationVariable Long roomId,
                               @AuthenticationPrincipal UserPrincipal user) {
        presenceService.userJoinedRoom(roomId, user.getId());
    }
}
```

**구독 토픽:**
- `/topic/rooms/{roomId}/messages` - 메시지 수신
- `/topic/rooms/{roomId}/typing` - 타이핑 상태
- `/topic/rooms/{roomId}/presence` - 입장/퇴장 알림

### 3.2 User WebSocket (글로벌 알림)
```java
@Controller
public class UserWebSocketController {

    @SubscribeMapping("/topic/users/{userId}/notifications")
    public void subscribeToUserNotifications(@DestinationVariable Long userId,
                                            @AuthenticationPrincipal UserPrincipal user) {
        globalNotificationService.subscribeUser(userId);
    }
}
```

**구독 토픽:**
- `/topic/users/{userId}/room-updates` - 읽지 않은 메시지 카운트
- `/topic/users/{userId}/notifications` - 푸시 알림
- `/topic/users/{userId}/friend-requests` - 친구 요청 (향후)

## 📊 4. Redis Streams 스키마 설계

### 4.1 Room Message Stream
```javascript
// Stream Key: room:{roomId}:messages
{
  "messageId": "msg_20240924_1234567890",
  "userId": 5678,
  "userInfo": {
    "nickname": "user123",
    "avatarUrl": "https://..."
  },
  "type": "TEXT", // TEXT, IMAGE, GIF, VIDEO, SYSTEM
  "content": {
    "text": "메시지 내용",
    "mediaUrl": "https://s3.../file.mp4",
    "thumbnailUrl": "https://s3.../thumb.jpg"
  },
  "clientTempId": "temp_abc123",
  "timestamp": "2024-09-24T10:25:00Z",
  "metadata": {
    "platform": "WEB",
    "version": "1.2.3"
  }
}
```

### 4.2 User Event Stream
```javascript
// Stream Key: user:{userId}:events
{
  "eventType": "UNREAD_UPDATE",
  "roomId": 1001,
  "data": {
    "unreadCount": 5,
    "lastMessage": {
      "text": "안녕하세요!",
      "senderNickname": "friend123",
      "timestamp": "2024-09-24T10:25:00Z"
    },
    "totalUnreadRooms": 3
  },
  "timestamp": "2024-09-24T10:25:01Z"
}
```

### 4.3 System Event Stream
```javascript
// Stream Key: system:events
{
  "eventType": "MAINTENANCE_NOTICE",
  "targetType": "ALL_USERS", // ALL_USERS, ROOM_USERS, SPECIFIC_USERS
  "targetIds": [], // 특정 대상이 있는 경우
  "data": {
    "title": "시스템 점검 안내",
    "message": "오늘 밤 12시부터 1시간 점검 예정",
    "priority": "HIGH"
  },
  "timestamp": "2024-09-24T10:25:00Z"
}
```

## ⚙️ 5. Consumer Group 설계

### 5.1 Room Message Consumers
```java
// Consumer Group: room-broadcast-group
@Component
public class RoomBroadcastConsumer {

    @StreamListener(value = "room:*:messages", group = "room-broadcast-group")
    public void handleRoomMessage(ObjectRecord<String, MessageDto> record) {
        String roomId = extractRoomId(record.getStream());
        MessageDto message = record.getValue();

        // 1. 해당 방에 연결된 로컬 WebSocket 세션 조회
        Set<String> localSessions = connectionManager.getLocalRoomSessions(roomId);

        // 2. WebSocket 브로드캐스트
        localSessions.forEach(sessionId ->
            webSocketService.sendToSession(sessionId, message)
        );

        // 3. ACK 처리
        redisTemplate.opsForStream().acknowledge(
            record.getStream(), "room-broadcast-group", record.getId()
        );
    }
}

// Consumer Group: room-storage-group
@Component
public class RoomStorageConsumer {

    @StreamListener(value = "room:*:messages", group = "room-storage-group")
    public void handleMessageStorage(ObjectRecord<String, MessageDto> record) {
        MessageDto message = record.getValue();

        // MongoDB에 영구 저장
        ChatMessage chatMessage = messageMapper.toEntity(message);
        chatMessageRepository.save(chatMessage);

        // 읽지 않은 카운트 업데이트 이벤트 발행
        unreadCountService.handleNewMessage(message);

        // ACK
        redisTemplate.opsForStream().acknowledge(
            record.getStream(), "room-storage-group", record.getId()
        );
    }
}
```

### 5.2 User Event Consumers
```java
// Consumer Group: user-notification-group
@Component
public class UserNotificationConsumer {

    @StreamListener(value = "user:*:events", group = "user-notification-group")
    public void handleUserEvent(ObjectRecord<String, UserEventDto> record) {
        String userId = extractUserId(record.getStream());
        UserEventDto event = record.getValue();

        // 해당 사용자의 로컬 WebSocket 세션 조회
        Set<String> userSessions = connectionManager.getLocalUserSessions(userId);

        if (!userSessions.isEmpty()) {
            // 온라인 상태: 즉시 WebSocket 전송
            userSessions.forEach(sessionId ->
                webSocketService.sendToSession(sessionId, event)
            );
        } else {
            // 오프라인 상태: 푸시 알림 처리
            pushNotificationService.sendPushNotification(userId, event);
        }

        // ACK
        redisTemplate.opsForStream().acknowledge(
            record.getStream(), "user-notification-group", record.getId()
        );
    }
}
```

## 🔑 6. Redis 키 전략 최적화

### 6.1 사용자 상태 관리
```javascript
// 온라인 사용자 (TTL: 5분)
presence:room:{roomId}:online → SET {userId1, userId2, ...}

// 방 멤버십 (영구)
presence:room:{roomId}:members → SET {userId1, userId2, ...}

// 사용자별 참여 방 목록 (영구)
user:{userId}:joined-rooms → SET {roomId1, roomId2, ...}
```

### 6.2 읽지 않은 메시지 관리
```javascript
// 사용자별 읽지 않은 메시지 카운트 (TTL: 30일)
user:{userId}:unread:{roomId} → COUNT

// 사용자별 마지막 읽은 메시지 (TTL: 30일)
user:{userId}:lastread:{roomId} → {messageId, timestamp}

// 방별 최신 메시지 캐시 (TTL: 1시간)
room:{roomId}:latest → {messageId, content, timestamp, senderInfo}
```

### 6.3 연결 관리
```javascript
// 서버별 WebSocket 연결 (TTL: 1시간)
websocket:server:{serverId}:room-sessions → HASH {sessionId: roomId}
websocket:server:{serverId}:user-sessions → HASH {sessionId: userId}

// 전역 사용자 연결 위치 (TTL: 1시간)
user:{userId}:connected-server → serverId
```

## 💡 7. 핵심 서비스 구현

### 7.1 MessageCoordinatorService
```java
@Service
@Slf4j
public class MessageCoordinatorService {

    private final MessageValidator messageValidator;
    private final MessageEnricher messageEnricher;
    private final RedisStreamsProducer streamsProducer;
    private final RateLimitService rateLimitService;

    @Transactional
    public MessageProcessResult processRoomMessage(Long roomId, Long userId, SendMessagePayload payload) {
        try {
            // 1. 레이트 리미팅 체크
            rateLimitService.checkRateLimit(userId, roomId);

            // 2. 메시지 검증
            ValidationResult validation = messageValidator.validate(payload);
            if (!validation.isValid()) {
                throw new MessageValidationException(validation.getErrors());
            }

            // 3. 메시지 강화 (메타데이터 추가)
            EnrichedMessage enrichedMessage = messageEnricher.enrich(payload, userId, roomId);

            // 4. Redis Streams에 발행
            String messageId = streamsProducer.sendToRoom(roomId, enrichedMessage);

            // 5. 클라이언트 ACK용 응답 생성
            return MessageProcessResult.success(messageId, enrichedMessage.getClientTempId());

        } catch (Exception e) {
            log.error("Failed to process room message: roomId={}, userId={}", roomId, userId, e);
            return MessageProcessResult.failure(e.getMessage());
        }
    }
}
```

### 7.2 UnreadCountService
```java
@Service
@Slf4j
public class UnreadCountService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final GlobalNotificationService globalNotificationService;
    private final RoomMemberService roomMemberService;
    private final PresenceService presenceService;

    @EventListener
    @Async("unreadCountExecutor")
    public void handleNewMessage(MessageStoredEvent event) {
        Long roomId = event.getRoomId();
        Long senderId = event.getSenderId();

        try {
            // 1. 해당 방의 모든 멤버 조회
            Set<Long> allMembers = roomMemberService.getAllMembers(roomId);

            // 2. 현재 온라인 멤버 조회
            Set<Long> onlineMembers = presenceService.getOnlineUsers(roomId);

            // 3. 오프라인 멤버들의 읽지 않은 카운트 증가
            Set<Long> offlineMembers = Sets.difference(allMembers, onlineMembers);
            offlineMembers.remove(senderId); // 발신자 제외

            // 4. 배치로 읽지 않은 카운트 업데이트
            updateUnreadCountsBatch(offlineMembers, roomId);

            // 5. 각 오프라인 사용자에게 알림 전송
            offlineMembers.forEach(userId ->
                globalNotificationService.notifyUnreadUpdate(userId, roomId, event.getMessage())
            );

        } catch (Exception e) {
            log.error("Failed to update unread counts for roomId: {}", roomId, e);
        }
    }

    private void updateUnreadCountsBatch(Set<Long> userIds, Long roomId) {
        // Redis Pipeline을 사용한 배치 업데이트
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            userIds.forEach(userId -> {
                String key = "user:" + userId + ":unread:" + roomId;
                connection.incr(key.getBytes());
                connection.expire(key.getBytes(), Duration.ofDays(30).getSeconds());
            });
            return null;
        });
    }

    public Map<Long, Integer> getUserUnreadCounts(Long userId) {
        // 사용자가 참여한 모든 방의 읽지 않은 카운트 조회
        Set<Long> joinedRooms = roomMemberService.getUserJoinedRooms(userId);

        Map<Long, Integer> unreadCounts = new HashMap<>();
        joinedRooms.forEach(roomId -> {
            String key = "user:" + userId + ":unread:" + roomId;
            Integer count = (Integer) redisTemplate.opsForValue().get(key);
            unreadCounts.put(roomId, count != null ? count : 0);
        });

        return unreadCounts;
    }
}
```

### 7.3 GlobalNotificationService
```java
@Service
@Slf4j
public class GlobalNotificationService {

    private final RedisStreamsProducer streamsProducer;
    private final RoomService roomService;
    private final UnreadCountService unreadCountService;

    public void notifyUnreadUpdate(Long userId, Long roomId, MessageDto lastMessage) {
        try {
            // 1. 읽지 않은 카운트 조회
            int unreadCount = unreadCountService.getUnreadCount(userId, roomId);

            // 2. 방 정보 조회
            RoomSummaryDto roomInfo = roomService.getRoomSummary(roomId);

            // 3. 사용자별 총 읽지 않은 방 수 계산
            int totalUnreadRooms = unreadCountService.getTotalUnreadRooms(userId);

            // 4. 알림 이벤트 생성
            UserEventDto event = UserEventDto.builder()
                .eventType("UNREAD_UPDATE")
                .roomId(roomId)
                .data(Map.of(
                    "unreadCount", unreadCount,
                    "roomInfo", roomInfo,
                    "lastMessage", lastMessage,
                    "totalUnreadRooms", totalUnreadRooms
                ))
                .timestamp(Instant.now())
                .build();

            // 5. 사용자별 이벤트 스트림에 발행
            streamsProducer.sendToUser(userId, event);

        } catch (Exception e) {
            log.error("Failed to notify unread update: userId={}, roomId={}", userId, roomId, e);
        }
    }

    public void notifySystemEvent(SystemEventDto event) {
        // 시스템 전체 알림
        streamsProducer.sendToSystem(event);
    }
}
```

## 📈 8. 성능 최적화 및 확장성

### 8.1 Connection Pooling
```java
@Configuration
public class WebSocketConnectionConfig {

    @Bean
    public WebSocketConnectionManager connectionManager() {
        return WebSocketConnectionManager.builder()
            .maxConnectionsPerUser(3)           // 사용자당 최대 연결 수
            .connectionTimeout(Duration.ofMinutes(30)) // 연결 타임아웃
            .heartbeatInterval(Duration.ofSeconds(30)) // 하트비트 간격
            .maxIdleTime(Duration.ofMinutes(10))       // 최대 유휴 시간
            .build();
    }

    @Bean("websocketExecutor")
    public Executor websocketExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(200);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("websocket-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

### 8.2 Redis Streams 최적화
```java
@Configuration
public class RedisStreamsOptimizationConfig {

    @Bean
    public StreamMessageListenerContainer<String, ObjectRecord<String, Object>>
            optimizedStreamContainer(RedisConnectionFactory connectionFactory) {

        StreamMessageListenerContainerOptions<String, ObjectRecord<String, Object>> options =
            StreamMessageListenerContainerOptions
                .builder()
                .batchSize(50)                          // 배치 크기 증가
                .pollTimeout(Duration.ofMillis(100))    // 폴링 간격 최적화
                .executor(streamProcessingExecutor())   // 전용 스레드 풀
                .errorHandler((ex, record) -> {         // 에러 핸들링
                    log.error("Stream processing error: {}", record, ex);
                })
                .build();

        return StreamMessageListenerContainer.create(connectionFactory, options);
    }

    @Bean("streamProcessingExecutor")
    public Executor streamProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("stream-consumer-");
        executor.initialize();
        return executor;
    }
}
```

### 8.3 MongoDB 샤딩 최적화
```javascript
// 메시지 컬렉션 샤딩 키 설정
db.adminCommand({
  shardCollection: "vibechat.messages",
  key: { "roomId": "hashed" }  // 해시 기반 균등 분산
});

// 복합 인덱스 생성 (쿼리 최적화)
db.messages.createIndex(
  { "roomId": 1, "timestamp": -1 },
  { name: "idx_room_timestamp" }
);

db.messages.createIndex(
  { "userId": 1, "timestamp": -1 },
  { name: "idx_user_timestamp" }
);
```

## 🔍 9. 모니터링 및 로깅

### 9.1 메트릭 수집
```java
@Component
public class ChatMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final Counter messagesSent;
    private final Timer messageProcessingTime;
    private final Gauge activeConnections;

    public ChatMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.messagesSent = Counter.builder("chat.messages.sent")
            .description("총 전송된 메시지 수")
            .register(meterRegistry);
        this.messageProcessingTime = Timer.builder("chat.message.processing.time")
            .description("메시지 처리 시간")
            .register(meterRegistry);
        this.activeConnections = Gauge.builder("chat.websocket.connections.active")
            .description("활성 WebSocket 연결 수")
            .register(meterRegistry, this, ChatMetricsCollector::getActiveConnectionCount);
    }

    public void recordMessageSent(String roomId, String messageType) {
        messagesSent.increment(
            Tags.of(
                "room_id", roomId,
                "message_type", messageType
            )
        );
    }

    private double getActiveConnectionCount() {
        // 활성 연결 수 계산 로직
        return connectionManager.getActiveConnectionCount();
    }
}
```

### 9.2 구조화된 로깅
```java
@Slf4j
public class ChatStructuredLogger {

    public void logMessageProcessed(String messageId, Long roomId, Long userId,
                                   String messageType, long processingTimeMs) {
        MDC.put("messageId", messageId);
        MDC.put("roomId", String.valueOf(roomId));
        MDC.put("userId", String.valueOf(userId));
        MDC.put("messageType", messageType);
        MDC.put("processingTimeMs", String.valueOf(processingTimeMs));

        log.info("Message processed successfully");

        MDC.clear();
    }

    public void logWebSocketConnection(String sessionId, Long userId, String action) {
        MDC.put("sessionId", sessionId);
        MDC.put("userId", String.valueOf(userId));
        MDC.put("action", action); // CONNECTED, DISCONNECTED, ERROR

        log.info("WebSocket connection event");

        MDC.clear();
    }
}
```

## 🎯 10. 성능 목표 및 SLA

### 10.1 성능 목표
- **메시지 전달 지연**: P95 < 100ms, P99 < 200ms
- **WebSocket 연결**: 동시 연결 10만개 (서버당 1만개 × 10대)
- **메시지 처리량**: 초당 10,000 메시지 처리
- **읽지 않은 카운트 업데이트**: < 50ms
- **시스템 가용성**: 99.9% (월 43분 이하 다운타임)

### 10.2 확장 계획
```
Phase 1 (현재): 단일 서버 클러스터
- Chat-Server: 3대
- Redis: 3 node cluster
- MongoDB: 3 node replica set

Phase 2 (6개월): 리전별 분산
- 서울 리전: Chat-Server 5대
- 부산 리전: Chat-Server 3대
- Cross-region replication

Phase 3 (1년): 글로벌 확장
- 아시아 태평양: 10대
- 북미: 5대
- 유럽: 5대
```

## 🛠️ 11. 마이크로 서비스 분리 및 리팩토링 전략

### 비즈니스 로직 마이크로 분리 설계
```
📁 현재 구조 → 개선 구조

MessageServiceImpl (단일 클래스, 모든 책임)
     ↓
┌─────────────────────────────────────────────────────────────┐
│                    Core Services                            │
├─────────────────────────────────────────────────────────────┤
│ MessageCoordinatorService    │ 메시지 처리 조정자          │
│ MessageValidator            │ 입력 검증 전담               │
│ MessageEnricher            │ 메타데이터 강화              │
│ RedisStreamsProducer       │ 스트림 발행 전담             │
├─────────────────────────────────────────────────────────────┤
│                   Consumer Services                         │
├─────────────────────────────────────────────────────────────┤
│ RoomBroadcastConsumer      │ 실시간 WebSocket 브로드캐스트 │
│ RoomStorageConsumer        │ MongoDB 영구 저장            │
│ UserNotificationConsumer   │ 오프라인 사용자 알림          │
│ AnalyticsConsumer          │ 통계 및 모니터링             │
├─────────────────────────────────────────────────────────────┤
│                  Support Services                          │
├─────────────────────────────────────────────────────────────┤
│ UnreadCountService         │ 읽지 않은 메시지 카운트       │
│ GlobalNotificationService  │ 글로벌 알림 처리             │
│ PresenceService           │ 사용자 온라인 상태 관리        │
│ ConnectionManager          │ WebSocket 연결 관리          │
└─────────────────────────────────────────────────────────────┘
```

## 📋 12. 구현 로드맵 및 우선순위

### 🥇 **1순위: 현재 로직 리팩토링 (3-4일)**

#### **Day 1: MessageService 분해**
- **현재**: `MessageServiceImpl` (148줄, 모든 책임)
- **목표**: 단일 책임 원칙 적용, 4개 클래스로 분리

**구체적 작업:**
```java
// 현재 MessageServiceImpl 제거 대상:
❌ MessageServiceImpl.broadcastMessage() - 검증+브로드캐스트+저장 모두 수행

// 새로 생성할 클래스들:
✅ MessageCoordinatorService.processMessage() - 조정만 담당
✅ MessageValidator.validate() - 검증만 담당
✅ MessageEnricher.enrich() - 메타데이터 강화만 담당
✅ RedisStreamsProducer.sendToRoom() - 스트림 발행만 담당
```

#### **Day 2: Consumer Group 분리**
- **현재**: `MessageQueueServiceImpl` (복잡한 단일 클래스)
- **목표**: Consumer별 독립 클래스 생성

**구체적 작업:**
```java
// 제거할 복잡한 로직:
❌ MessageQueueServiceImpl - 모든 Consumer 로직 혼재

// 새로 생성:
✅ RoomBroadcastConsumer - WebSocket 브로드캐스트만
✅ RoomStorageConsumer - MongoDB 저장만
✅ UserNotificationConsumer - 오프라인 알림만
✅ AnalyticsConsumer - 통계 수집만
```

#### **Day 3: WebSocket Controller 정리**
- **현재**: `ChatWsController` - 단일 WebSocket 처리
- **목표**: Room/User WebSocket 분리

**구체적 작업:**
```java
// 분리 전:
❌ ChatWsController - 모든 WebSocket 처리

// 분리 후:
✅ RoomWebSocketController - 채팅 메시지 처리
✅ UserWebSocketController - 글로벌 알림 처리
✅ WebSocketConnectionManager - 연결 생명주기 관리
```

#### **Day 4: 불필요한 코드 제거**
- **Domain 정리**: 사용하지 않는 엔티티/DTO 제거
- **Service 정리**: 중복 로직 제거
- **Config 통합**: 분산된 설정 파일 통합

### 🥈 **2순위: 읽지 않은 카운트 시스템 (2-3일)**

#### **Day 5-6: UnreadCountService 구현**
```java
// 핵심 비즈니스 로직
@Service
public class UnreadCountService {
    // Redis 키 전략 구현
    // 배치 업데이트 로직
    // TTL 관리
    // 성능 최적화
}
```

#### **Day 7: GlobalNotificationService 구현**
```java
// 사용자별 실시간 알림
@Service
public class GlobalNotificationService {
    // User Event Stream 발행
    // 오프라인 사용자 처리
    // 시스템 전체 알림
}
```

### 🥉 **3순위: Redis Streams 최적화 (2-3일)**

#### **Day 8-9: Connection Pooling & 성능 최적화**
- WebSocket 연결 풀 관리
- Redis Streams Consumer 성능 튜닝
- MongoDB 샤딩 설정

#### **Day 10: 모니터링 및 테스트**
- 메트릭 수집 시스템
- 부하 테스트 (100방 × 50명)
- 장애 복구 테스트

## 📝 13. 상세 리팩토링 체크리스트

### 1순위 상세 작업 목록

#### **MessageCoordinatorService 생성**
```java
// 파일: /chat-server/src/main/java/com/vibechat/service/coordinator/MessageCoordinatorService.java
@Service
public class MessageCoordinatorService {

    private final MessageValidator validator;
    private final MessageEnricher enricher;
    private final RedisStreamsProducer streamsProducer;
    private final RateLimitService rateLimitService;

    @Transactional
    public MessageProcessResult processRoomMessage(Long roomId, Long userId, SendMessagePayload payload) {
        // 1. 레이트 리미팅 체크
        // 2. 메시지 검증
        // 3. 메시지 강화
        // 4. Redis Streams 발행
        // 5. 결과 반환
    }
}
```

#### **MessageValidator 생성**
```java
// 파일: /chat-server/src/main/java/com/vibechat/service/validation/MessageValidator.java
@Component
public class MessageValidator {

    public ValidationResult validate(SendMessagePayload payload) {
        // XSS 검증
        // 길이 제한 검증
        // 미디어 타입 검증
        // 필수 필드 검증
    }
}
```

#### **MessageEnricher 생성**
```java
// 파일: /chat-server/src/main/java/com/vibechat/service/enrichment/MessageEnricher.java
@Component
public class MessageEnricher {

    public EnrichedMessage enrich(SendMessagePayload payload, Long userId, Long roomId) {
        // 타임스탬프 추가
        // 사용자 정보 첨부
        // 고유 ID 생성
        // 플랫폼 정보 추가
    }
}
```

#### **Consumer 클래스들 생성**
```java
// 파일: /chat-server/src/main/java/com/vibechat/consumer/
├── RoomBroadcastConsumer.java       # WebSocket 브로드캐스트만 담당
├── RoomStorageConsumer.java         # MongoDB 저장만 담당
├── UserNotificationConsumer.java    # 오프라인 알림만 담당
└── AnalyticsConsumer.java          # 통계 수집만 담당
```

### 제거 대상 파일/로직

#### **완전 제거:**
```
❌ MessageServiceImpl.java - 너무 복잡, 완전 재작성
❌ WebSocketMessagingConfig.java - 순환참조 해결용, 더이상 불필요
❌ 사용하지 않는 DTO들 - StreamMessageDto 등
```

#### **대폭 수정:**
```
🔄 ChatWsController.java - Room/User 분리로 50% 축소
🔄 MessageQueueServiceImpl.java - Consumer 분리로 80% 축소
🔄 WebSocketConfig.java - 이중 WebSocket 지원으로 확장
```

## 📊 14. 리팩토링 진행 상황 체크포인트

### Day 1 완료 기준
- [ ] MessageCoordinatorService 구현 완료
- [ ] MessageValidator 구현 완료
- [ ] MessageEnricher 구현 완료
- [ ] 기존 MessageServiceImpl 제거
- [ ] 단위 테스트 3개 이상 작성

### Day 2 완료 기준
- [ ] 4개 Consumer 클래스 구현
- [ ] Redis Streams 연결 테스트 완료
- [ ] 기존 MessageQueueServiceImpl 80% 축소
- [ ] Consumer Group 정상 작동 확인

### Day 3 완료 기준
- [ ] RoomWebSocketController 분리
- [ ] UserWebSocketController 분리
- [ ] 기존 ChatWsController 50% 축소
- [ ] WebSocket 연결 테스트 완료

### Day 4 완료 기준
- [ ] 사용하지 않는 파일 10개 이상 제거
- [ ] 코드 중복도 50% 이하로 감소
- [ ] 전체 빌드 및 통합 테스트 통과
- [ ] 성능 저하 없음 확인

## 📝 12. 결론

본 아키텍처는 Redis Streams를 단일 메시지 처리 백본으로 사용하여 **카카오톡 수준의 확장성과 안정성**을 제공합니다.

### 핵심 장점
1. **단순성**: Redis Streams만 사용하여 복잡도 최소화
2. **확장성**: 서버 클러스터링으로 무제한 수평 확장
3. **안정성**: Consumer Group의 장애 복구 메커니즘
4. **실시간성**: P95 100ms 이하의 메시지 전달 지연

### 예상 성과
- **동시 접속**: 10만명 지원
- **메시지 처리**: 초당 1만건
- **방 수**: 1만개 동시 활성화
- **시스템 가용성**: 99.9%

이 아키텍처로 **대규모 실시간 채팅 서비스**를 안정적으로 운영할 수 있습니다. 🚀

---
*마지막 업데이트: 2024-09-24*
*다음 리뷰: 2024-10-01*