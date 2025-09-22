# 🔧 VibeChat 메시지 시스템 TRD (Technical Requirements Document)
**Version:** 2.0
**Date:** 2025-09-22
**Based on:** PRD_VIBECHAT_MESSAGE_SYSTEM.md v2.0

---

## 📋 기술 스택 & 아키텍처

### **현재 기술 스택**
```yaml
Backend:
  - API-Server: Spring Boot 3.3.4, Java 21, Maven
  - Chat-Server: Spring Boot 3.3.4, Java 21, Maven
  - Database: MySQL 8.0
  - Cache/Queue: Redis 7.x
  - File Storage: Local → S3 + CloudFront (Migration)

Frontend:
  - Framework: React 18.2.0, TypeScript 5.0
  - Build Tool: Vite 4.4.5
  - State Management: Redux Toolkit 1.9.5
  - Styling: Tailwind CSS 3.3.0
  - WebSocket: STOMP.js 2.3.3

Infrastructure:
  - Containerization: Docker Compose
  - Reverse Proxy: Nginx 1.25
  - Monitoring: Micrometer (Planned)
  - CI/CD: GitHub Actions (Planned)
```

### **마이크로서비스 분리 원칙**
```yaml
API-Server (Port 8080):
  Responsibilities:
    - REST API endpoints (/api/*)
    - File upload and media processing
    - User authentication & authorization
    - Business logic (Rooms, Users, Messages CRUD)
    - S3 integration
    - Redis event publishing

  Not Responsible For:
    - WebSocket connections
    - Real-time message broadcasting
    - Presence management
    - Typing indicators

Chat-Server (Port 8081):
  Responsibilities:
    - WebSocket/STOMP connections (/ws)
    - Real-time message broadcasting
    - Presence tracking
    - Typing indicators
    - Redis Streams message production
    - Redis event subscription

  Not Responsible For:
    - REST API endpoints
    - File storage
    - User management
    - Business logic validation
```

---

## 🎯 Redis Streams 메시지 큐 설계

### **1. Redis Streams 구조**

#### **Stream Keys & Consumer Groups**
```yaml
Streams:
  messages:{roomId}:
    Description: "방별 메시지 스트림"
    Max Length: 10000  # MAXLEN ~ 10000
    Consumer Groups:
      - message-processors
      - analytics-processors
      - notification-processors

  events:media:upload:
    Description: "미디어 업로드 완료 이벤트"
    Consumer Groups:
      - media-broadcast-processors

  events:presence:{roomId}:
    Description: "방별 접속 상태 이벤트"
    Consumer Groups:
      - presence-processors

Pub/Sub Channels:
  room:{roomId}:messages: "실시간 메시지 브로드캐스트"
  room:{roomId}:typing: "타이핑 상태 브로드캐스트"
  room:{roomId}:presence: "접속 상태 브로드캐스트"
  media:message:event: "미디어 메시지 이벤트"
```

#### **Message Data Structure**
```json
// Redis Stream Entry Structure
{
  "messageId": "auto-generated-stream-id",
  "roomId": 1,
  "userId": 101,
  "userNickname": "김채팅",
  "type": "TEXT",
  "contentText": "안녕하세요!",
  "mediaUrl": null,
  "mediaThumbUrl": null,
  "mediaDurationSec": null,
  "clientTempId": "uuid-4-from-client",
  "timestamp": "1640995200000",
  "processingStatus": "PENDING"  // PENDING, PROCESSED, FAILED
}
```

### **2. Spring Boot Redis Streams 구현**

#### **Configuration**
```java
// RedisStreamsConfig.java
@Configuration
@EnableScheduling
public class RedisStreamsConfig {

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>>
           streamMessageListenerContainer(RedisTemplate<String, Object> redisTemplate) {

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .batchSize(50)
                        .executor(Executors.newFixedThreadPool(4))
                        .errorHandler(new StreamMessageErrorHandler())
                        .build();

        return StreamMessageListenerContainer.create(
                redisTemplate.getConnectionFactory(), options);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

#### **Message Producer (Chat-Server)**
```java
// MessageStreamProducer.java
@Service
@RequiredArgsConstructor
public class MessageStreamProducer {

    private final StringRedisTemplate redisTemplate;
    private static final String STREAM_KEY_PREFIX = "messages:room:";

    @Async
    public CompletableFuture<String> publishMessage(SendMessagePayload payload, Long roomId, Long userId, String nickname) {

        String streamKey = STREAM_KEY_PREFIX + roomId;

        Map<String, String> messageData = Map.of(
            "roomId", roomId.toString(),
            "userId", userId.toString(),
            "userNickname", nickname,
            "type", payload.getType(),
            "contentText", Objects.toString(payload.getContentText(), ""),
            "mediaUrl", Objects.toString(payload.getMediaUrl(), ""),
            "clientTempId", payload.getClientTempId(),
            "timestamp", String.valueOf(System.currentTimeMillis()),
            "processingStatus", "PENDING"
        );

        // Add to Redis Stream
        RecordId recordId = redisTemplate.opsForStream()
                .add(StreamRecords.newRecord()
                        .ofMap(messageData)
                        .withStreamKey(streamKey));

        // Ensure consumer group exists
        ensureConsumerGroup(streamKey, "message-processors");

        return CompletableFuture.completedFuture(recordId.getValue());
    }

    private void ensureConsumerGroup(String streamKey, String groupName) {
        try {
            redisTemplate.opsForStream().createGroup(streamKey, groupName, ReadOffset.from("0"));
        } catch (Exception e) {
            // Group already exists, ignore
        }
    }
}
```

#### **Message Consumer (Background Workers)**
```java
// MessageStreamConsumer.java
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageStreamConsumer {

    private final MessageService messageService;
    private final StringRedisTemplate redisTemplate;

    @StreamListener(value = "messages:room:*", group = "message-processors")
    public void handleMessage(MapRecord<String, String, String> record) {

        try {
            // Extract message data
            Map<String, String> messageData = record.getValue();

            // Convert to domain object
            Message message = convertToMessage(messageData);

            // Save to MySQL
            Message savedMessage = messageService.saveMessage(message);

            // Mark as processed
            markAsProcessed(record.getStream(), record.getId().getValue());

            // Acknowledge processing
            redisTemplate.opsForStream().acknowledge(
                record.getStream(), "message-processors", record.getId());

            log.info("Message processed successfully: {}", savedMessage.getId());

        } catch (Exception e) {
            log.error("Failed to process message: {}", record, e);
            // Move to Dead Letter Queue
            moveToDeadLetterQueue(record);
        }
    }

    @Scheduled(fixedDelay = 60000) // Every minute
    public void processFailedMessages() {
        // Process messages from Dead Letter Queue
        // Retry logic with exponential backoff
    }
}
```

---

## 🚀 WebSocket 실시간 통신 설계

### **STOMP Configuration**
```java
// WebSocketConfig.java (Chat-Server)
@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Simple broker for /topic destinations
        config.enableSimpleBroker("/topic", "/queue")
              .setHeartbeatValue(new long[]{10000, 10000}); // 10s heartbeat

        // Prefix for app destinations
        config.setApplicationDestinationPrefixes("/app");

        // User-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setSessionCookieNeeded(false)
                .setHeartbeatTime(25000)
                .setDisconnectDelay(5000);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Rate limiting
        registration.interceptors(new RateLimitingChannelInterceptor());
        registration.taskExecutor()
                   .corePoolSize(4)
                   .maxPoolSize(16)
                   .queueCapacity(1000);
    }
}
```

### **Message Handler Implementation**
```java
// ChatMessageController.java (Chat-Server)
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatMessageController {

    private final MessageStreamProducer messageProducer;
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;
    private final RateLimitService rateLimitService;

    @MessageMapping("/rooms/{roomId}/send")
    @SendTo("/topic/rooms/{roomId}/messages")
    public CompletableFuture<WebSocketMessageResponse> sendMessage(
            @DestinationVariable Long roomId,
            SendMessagePayload payload,
            StompHeaderAccessor headerAccessor) {

        // Extract user info from session
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
        String nickname = (String) headerAccessor.getSessionAttributes().get("nickname");

        // Rate limiting check
        if (!rateLimitService.allowRequest(userId, roomId)) {
            throw new RateLimitExceededException("Too many messages");
        }

        // Validate message
        validateMessage(payload, userId, roomId);

        // Create immediate response for optimistic UI update
        WebSocketMessageResponse immediateResponse = WebSocketMessageResponse.builder()
                .id(null) // Will be set after DB save
                .clientTempId(payload.getClientTempId())
                .roomId(roomId)
                .user(UserSummaryDto.builder()
                      .id(userId)
                      .nickname(nickname)
                      .build())
                .type(payload.getType())
                .contentText(payload.getContentText())
                .mediaUrl(payload.getMediaUrl())
                .mediaThumbUrl(payload.getMediaThumbUrl())
                .createdAt(Instant.now().toString())
                .build();

        // Async processing
        return messageProducer.publishMessage(payload, roomId, userId, nickname)
                .thenApply(streamId -> {
                    // Update presence (last activity)
                    presenceService.updateUserActivity(userId, roomId);
                    return immediateResponse;
                })
                .exceptionally(throwable -> {
                    log.error("Failed to publish message", throwable);
                    throw new RuntimeException("Message processing failed");
                });
    }

    @MessageMapping("/rooms/{roomId}/typing")
    public void handleTyping(
            @DestinationVariable Long roomId,
            TypingPayload payload,
            StompHeaderAccessor headerAccessor) {

        String nickname = (String) headerAccessor.getSessionAttributes().get("nickname");
        Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");

        // Update typing state in Redis
        presenceService.updateTypingState(userId, roomId, payload.isTyping());

        // Broadcast typing state
        TypingResponse response = TypingResponse.builder()
                .nickname(nickname)
                .typing(payload.isTyping())
                .build();

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomId + "/typing", response);
    }
}
```

---

## 📁 파일 업로드 & S3 통합

### **S3 Configuration**
```yaml
# application.yml (API-Server)
aws:
  s3:
    bucket-name: vibechat-media-prod
    region: ap-northeast-2
    access-key-id: ${AWS_ACCESS_KEY_ID}
    secret-access-key: ${AWS_SECRET_ACCESS_KEY}
  cloudfront:
    domain: d1234567890.cloudfront.net

file:
  upload:
    max-size: 52428800  # 50MB
    allowed-types: image/jpeg,image/png,image/gif,video/mp4,video/quicktime
    temp-dir: /tmp/vibechat-uploads
```

### **File Upload Service**
```java
// S3FileUploadService.java (API-Server)
@Service
@RequiredArgsConstructor
@Slf4j
public class S3FileUploadService {

    private final AmazonS3 s3Client;
    private final FFmpegThumbnailService thumbnailService;
    private final MediaEventPublisher eventPublisher;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.cloudfront.domain}")
    private String cloudFrontDomain;

    @Async
    public CompletableFuture<MediaUploadResult> uploadMedia(
            MultipartFile file, Long roomId, Long userId, String userNickname) {

        try {
            // 1. Validate file
            validateFile(file);

            // 2. Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = FilenameUtils.getExtension(originalFilename);
            String uniqueFilename = generateUniqueFilename(roomId, fileExtension);

            // 3. Create temporary local file
            File tempFile = createTempFile(file, uniqueFilename);

            // 4. Generate thumbnail (if applicable)
            String thumbnailUrl = null;
            Integer durationSec = null;

            if (isImage(file)) {
                thumbnailUrl = generateImageThumbnail(tempFile, uniqueFilename);
            } else if (isVideo(file)) {
                thumbnailUrl = generateVideoThumbnail(tempFile, uniqueFilename);
                durationSec = extractVideoDuration(tempFile);
            }

            // 5. Upload original file to S3
            String mediaUrl = uploadToS3(tempFile, uniqueFilename, file.getContentType());

            // 6. Save message to database
            Message message = saveMessageToDatabase(roomId, userId, userNickname,
                    mediaUrl, thumbnailUrl, durationSec, getMessageType(file));

            // 7. Publish event to Chat-Server
            eventPublisher.publishMediaMessageEvent(message);

            // 8. Cleanup
            cleanupTempFiles(tempFile);

            return CompletableFuture.completedFuture(
                    MediaUploadResult.builder()
                            .messageId(message.getId())
                            .mediaUrl(mediaUrl)
                            .thumbnailUrl(thumbnailUrl)
                            .durationSec(durationSec)
                            .build());

        } catch (Exception e) {
            log.error("Failed to upload media", e);
            throw new MediaUploadException("Upload failed: " + e.getMessage());
        }
    }

    private String uploadToS3(File file, String filename, String contentType) {
        String key = "rooms/" + filename;

        PutObjectRequest request = new PutObjectRequest(bucketName, key, file)
                .withCannedAcl(CannedAccessControlList.PublicRead)
                .withMetadata(createMetadata(contentType));

        s3Client.putObject(request);

        return "https://" + cloudFrontDomain + "/" + key;
    }
}
```

---

## ⚡ 성능 최적화 설계

### **1. 프론트엔드 최적화**

#### **Virtual Scrolling Implementation**
```typescript
// VirtualizedMessageList.tsx
import { FixedSizeList as List } from 'react-window';

interface VirtualizedMessageListProps {
  messages: Message[];
  height: number;
}

const ITEM_HEIGHT = 80; // Average message height

export const VirtualizedMessageList: React.FC<VirtualizedMessageListProps> = ({
  messages,
  height
}) => {
  const renderMessage = useCallback(({ index, style }: ListChildComponentProps) => (
    <div style={style}>
      <MessageBubble message={messages[index]} />
    </div>
  ), [messages]);

  return (
    <List
      height={height}
      itemCount={messages.length}
      itemSize={ITEM_HEIGHT}
      overscanCount={10} // Render extra items for smooth scrolling
    >
      {renderMessage}
    </List>
  );
};
```

#### **Image Lazy Loading**
```typescript
// LazyImage.tsx
import { useState, useRef, useEffect } from 'react';

interface LazyImageProps {
  src: string;
  thumbnail: string;
  alt: string;
  className?: string;
}

export const LazyImage: React.FC<LazyImageProps> = ({ src, thumbnail, alt, className }) => {
  const [isLoaded, setIsLoaded] = useState(false);
  const [isInView, setIsInView] = useState(false);
  const imgRef = useRef<HTMLImageElement>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsInView(true);
          observer.disconnect();
        }
      },
      { threshold: 0.1 }
    );

    if (imgRef.current) {
      observer.observe(imgRef.current);
    }

    return () => observer.disconnect();
  }, []);

  return (
    <div className={`relative ${className}`} ref={imgRef}>
      {/* Thumbnail (always loaded) */}
      <img
        src={thumbnail}
        alt={alt}
        className={`w-full h-full object-cover transition-opacity duration-300 ${
          isLoaded ? 'opacity-0' : 'opacity-100'
        }`}
      />

      {/* Full resolution image (lazy loaded) */}
      {isInView && (
        <img
          src={src}
          alt={alt}
          className={`absolute inset-0 w-full h-full object-cover transition-opacity duration-300 ${
            isLoaded ? 'opacity-100' : 'opacity-0'
          }`}
          onLoad={() => setIsLoaded(true)}
        />
      )}
    </div>
  );
};
```

### **2. 백엔드 최적화**

#### **Connection Pooling**
```yaml
# application.yml
spring:
  datasource:
    hikari:
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
      maximum-pool-size: 20
      minimum-idle: 5
      pool-name: VibeChatHikariPool

  data:
    redis:
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2
          max-wait: -1ms
      timeout: 3000ms
```

#### **JPA Batch Processing**
```java
// MessageRepository.java
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO messages (room_id, user_id, type, content_text, media_url,
                             media_thumb_url, media_duration_sec, created_at)
        VALUES (:#{#messages})
        """, nativeQuery = true)
    @BatchSize(50)
    void batchInsertMessages(@Param("messages") List<Message> messages);

    @Query(value = """
        SELECT m.* FROM messages m
        WHERE m.room_id = :roomId
        AND (:beforeId IS NULL OR m.id < :beforeId)
        ORDER BY m.created_at DESC
        LIMIT :limit
        """)
    List<Message> findByRoomIdWithPagination(
        @Param("roomId") Long roomId,
        @Param("beforeId") Long beforeId,
        @Param("limit") int limit);
}
```

---

## 📊 모니터링 & 관찰성

### **메트릭 수집**
```java
// MetricsConfig.java
@Configuration
public class MetricsConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
                "application", "vibechat",
                "version", getClass().getPackage().getImplementationVersion());
    }
}

// Custom metrics
@Service
@RequiredArgsConstructor
public class MessageMetricsService {

    private final MeterRegistry meterRegistry;
    private final Timer.Sample messageProcessingTimer;
    private final Counter messagesSentCounter;
    private final Counter messagesFailedCounter;
    private final Gauge activeWebSocketConnections;

    @EventListener
    public void handleMessageSent(MessageSentEvent event) {
        messagesSentCounter.increment(
                Tags.of("room", event.getRoomId().toString(),
                       "type", event.getMessageType()));
    }

    @EventListener
    public void handleMessageFailed(MessageFailedEvent event) {
        messagesFailedCounter.increment(
                Tags.of("error", event.getErrorType()));
    }
}
```

### **Health Checks**
```java
// CustomHealthIndicator.java
@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // Check Redis connectivity
        // Check MySQL connectivity
        // Check S3 connectivity
        // Check WebSocket broker status

        return Health.up()
                .withDetail("redis", checkRedisHealth())
                .withDetail("mysql", checkMySQLHealth())
                .withDetail("s3", checkS3Health())
                .withDetail("websocket", checkWebSocketHealth())
                .build();
    }
}
```

---

## 🔒 보안 요구사항

### **Rate Limiting**
```java
// RateLimitService.java
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    // 20 messages per minute per user per room
    private static final int MAX_MESSAGES_PER_MINUTE = 20;
    private static final int WINDOW_SIZE_SECONDS = 60;

    public boolean allowRequest(Long userId, Long roomId) {
        String key = String.format("rate_limit:msg:%d:%d", userId, roomId);

        // Sliding window rate limiting using Redis
        long now = System.currentTimeMillis();
        long windowStart = now - (WINDOW_SIZE_SECONDS * 1000);

        // Remove old entries
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // Count current requests
        Long currentCount = redisTemplate.opsForZSet().count(key, windowStart, now);

        if (currentCount >= MAX_MESSAGES_PER_MINUTE) {
            return false;
        }

        // Add current request
        redisTemplate.opsForZSet().add(key, UUID.randomUUID().toString(), now);
        redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SIZE_SECONDS));

        return true;
    }
}
```

### **Input Validation & Sanitization**
```java
// MessageValidationService.java
@Service
public class MessageValidationService {

    private final Pattern SAFE_TEXT_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\p{P}\\p{Z}\\p{S}]{1,2000}$");
    private final List<String> BLOCKED_WORDS = loadBlockedWords();

    public void validateTextMessage(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new InvalidMessageException("Message content cannot be empty");
        }

        if (content.length() > 2000) {
            throw new InvalidMessageException("Message too long");
        }

        if (!SAFE_TEXT_PATTERN.matcher(content).matches()) {
            throw new InvalidMessageException("Message contains invalid characters");
        }

        // Check for blocked words
        if (containsBlockedWords(content)) {
            throw new InvalidMessageException("Message contains inappropriate content");
        }
    }

    public String sanitizeHtmlContent(String content) {
        return Jsoup.clean(content, Safelist.none());
    }
}
```

---

## 🧪 테스트 전략

### **Unit Test Coverage Goals**
- **Service Classes**: 90%+ coverage
- **Controllers**: 85%+ coverage
- **Repositories**: 70%+ coverage
- **Utilities**: 95%+ coverage

### **Integration Tests**
```java
// MessageIntegrationTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext
class MessageIntegrationTest {

    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");

    @Container
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0");

    @Test
    void shouldSendAndReceiveMessage() {
        // Given: User connected to WebSocket
        // When: Message sent through WebSocket
        // Then: Message received by other users
        // And: Message saved to database
        // And: Message added to Redis Stream
    }

    @Test
    void shouldHandleHighLoadMessaging() {
        // Simulate 100 concurrent users sending messages
        // Verify no message loss
        // Verify message order preservation
    }
}
```

### **Performance Test Requirements**
```yaml
Load Testing Scenarios:
  - Concurrent Users: 1,000
  - Messages per Second: 500
  - File Uploads per Minute: 100
  - Test Duration: 30 minutes

Acceptance Criteria:
  - Message Latency p95: ≤ 1 second
  - Error Rate: ≤ 0.1%
  - Memory Usage: Stable (no leaks)
  - CPU Usage: ≤ 80%
```

---

## 🚀 배포 & DevOps

### **Docker Configuration**
```dockerfile
# Dockerfile.chat-server
FROM openjdk:21-jdk-slim

WORKDIR /app
COPY target/chat-server-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### **Docker Compose Production**
```yaml
# docker-compose.prod.yml
version: '3.8'
services:
  api-server:
    build:
      context: ./api-server
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
      - REDIS_HOST=redis
      - AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
      - AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
    depends_on:
      - mysql
      - redis
    deploy:
      replicas: 2
      resources:
        limits:
          memory: 1G
          cpus: '0.5'

  chat-server:
    build:
      context: ./chat-server
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - REDIS_HOST=redis
    depends_on:
      - redis
    deploy:
      replicas: 2
      resources:
        limits:
          memory: 512M
          cpus: '0.3'

  nginx:
    image: nginx:alpine
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    ports:
      - "80:80"
      - "443:443"
    depends_on:
      - api-server
      - chat-server
```

---

## 📈 확장성 고려사항

### **수평 확장 전략**
```yaml
Current State (Single Instance):
  - API-Server: 1 instance
  - Chat-Server: 1 instance
  - MySQL: 1 primary
  - Redis: 1 standalone

Target State (Multi-Instance):
  - API-Server: 2-4 instances (behind load balancer)
  - Chat-Server: 2-4 instances (sticky sessions)
  - MySQL: 1 primary + 2 read replicas
  - Redis: 3-node cluster (master-slave)

Future State (High Scale):
  - API-Server: Auto-scaling (2-10 instances)
  - Chat-Server: Auto-scaling with Redis Cluster
  - MySQL: Sharded (by room_id % N)
  - Redis: 6-node cluster + Redis Streams partitioning
```

### **Database Scaling**
```sql
-- Partitioning Strategy
CREATE TABLE messages_2025_01 PARTITION OF messages
FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

-- Read Replica Routing
@Transactional(readOnly = true)
public List<Message> getMessages(Long roomId, Long beforeId, int limit) {
    // Automatically routes to read replica
}
```

---

**이 TRD는 실제 구현 시 참조할 수 있는 완전한 기술 명세서입니다. 모든 코드 예제는 실제 프로젝트 구조에 맞게 조정하여 사용하시기 바랍니다.**