# 🚀 VibeChat - 마이크로서비스 기반 실시간 채팅 아키텍처

## 📊 시스템 개요

### **아키텍처 핵심 원칙**
- **서버 분리**: WebSocket 채팅 서버와 REST API 서버 완전 분리
- **수평 확장**: 각 서비스 독립적 스케일링 지원
- **메시지 큐 중심**: Redis Pub/Sub 기반 분산 메시지 라우팅
- **고가용성**: 무손실 메시지 전달 보장

### **목표 성능 지표**
- **메시지 지연**: p95 ≤ 1초
- **동시 접속**: 1,000명 이상 지원
- **처리량**: 초당 10,000 메시지
- **가용성**: 99.9% 업타임

---

## 🏗️ **Phase 1: 마이크로서비스 아키텍처 구성**

### **1.1 서비스 분리 구조**
```
🌐 클라이언트 계층
├─ React Frontend (포트 3000)
├─ Mobile App (React Native/Flutter)
└─ Web Push Notifications

📡 로드 밸런서 계층
├─ Nginx Proxy (포트 80/443)
├─ WebSocket LB: ws://chat-servers (Sticky Session 불필요)
└─ API LB: http://api-server (Round Robin)

🔥 WebSocket 채팅 서버 클러스터 (Node.js + Socket.io)
├─ Chat Server 1 (포트 3001)
├─ Chat Server 2 (포트 3002)
├─ Chat Server 3 (포트 3003)
├─ ... (수평 확장 가능)
└─ 역할: 실시간 WebSocket 연결 관리 + 메시지 라우팅

🏢 REST API 서버 (Spring Boot)
├─ API Server (포트 8000)
├─ 역할: 인증/인가, 비즈니스 로직, 방 관리
├─ JWT 토큰 발급 및 검증
└─ 메시지 히스토리 API 제공

🚀 Message Queue 시스템 (Redis Cluster)
├─ Redis Master 1-3 (포트 6379-6381)
├─ Redis Slave 1-3 (포트 6382-6384)
├─ 개별 사용자 큐: queue:user:{userId}
├─ 방 멤버십 캐시: room:{roomId}:members
└─ 연결 정보: connections:{userId} → {serverId, socketId}

💾 데이터베이스 계층
├─ Message DB (MongoDB Cluster)
│   ├─ 메시지 영구 저장
│   ├─ 샤딩: roomId 기준
│   └─ 복제: 3-replica set
├─ User/Room DB (PostgreSQL)
│   ├─ 사용자 정보, 방 정보
│   ├─ Master-Slave 구성
│   └─ 읽기 전용 복제본
└─ Cache Layer (Redis)
    ├─ 세션 캐시
    ├─ 방 멤버 캐시
    └─ 온라인 사용자 상태

📱 Push Notification Service
├─ FCM Gateway (Android)
├─ APNs Gateway (iOS)
├─ Web Push Service
└─ 오프라인 사용자 감지 및 알림
```

### **1.2 메시지 전달 플로우 설계**

#### **1.2.1 1:1 채팅 플로우**
```
💬 User A → User B 메시지 전달
├─ 1. User A가 Chat Server 1에 메시지 전송
├─ 2. Chat Server 1이 Redis에 발행: PUBLISH queue:user:B "message"
├─ 3. User B가 연결된 Chat Server 3이 구독중: SUBSCRIBE queue:user:B
├─ 4. Chat Server 3이 User B에게 WebSocket으로 즉시 전달
├─ 5. 백그라운드: Message DB에 영구 저장
└─ 6. User B 오프라인 시: Push Notification 트리거
```

#### **1.2.2 그룹 채팅 플로우**
```
👥 User A → 그룹 채팅 메시지 전달
├─ 1. User A가 Chat Server 1에 그룹 메시지 전송
├─ 2. Chat Server 1이 Redis에서 그룹 멤버 조회: SMEMBERS room:123:members
├─ 3. 각 멤버별 개별 큐에 메시지 발행:
│   ├─ PUBLISH queue:user:B "group_message"
│   ├─ PUBLISH queue:user:C "group_message"
│   └─ PUBLISH queue:user:D "group_message"
├─ 4. 온라인 멤버들이 연결된 Chat Server들이 즉시 수신
├─ 5. 오프라인 멤버들에게 Push Notification
└─ 6. Message DB에 그룹 메시지 영구 저장
```

#### **1.2.3 연결 관리 및 라우팅**
```
🔌 WebSocket 연결 생명주기
├─ 1. 클라이언트가 JWT 토큰으로 WebSocket 연결 요청
├─ 2. Chat Server가 API Server에 토큰 검증 요청
├─ 3. 검증 성공 시 연결 정보 Redis 저장:
│   └─ HSET connections:userId "serverId" "chat-server-1" "socketId" "abc123"
├─ 4. 사용자 개별 큐 구독 시작: SUBSCRIBE queue:user:userId
├─ 5. 방 입장 시 멤버십 캐시 업데이트: SADD room:roomId:members userId
├─ 6. 연결 해제 시 정리:
│   ├─ Redis 연결 정보 삭제
│   ├─ 큐 구독 해제
│   └─ 방 멤버십에서 제거 (다른 연결 없을 시)
└─ 7. 하트비트: 30초마다 PING/PONG 교환
```

---

## **💾 Phase 2: 메시지 저장 및 배치 처리 아키텍처**

### **2.1 3-Layer 메시지 처리**

#### **2.1.1 Layer 1: 실시간 메시지 큐 (Redis Streams + Pub/Sub)**
```
⚡ 실시간 메시지 라우팅
├─ 2.1.1.1 WebSocket 메시지 수신 (Chat Server)
│   ├─ 기본 검증: 인증, 방 권한, 메시지 형식
│   ├─ Rate Limiting: userId 기준 20msg/min (Redis Sliding Window)
│   └─ 통과 시: Redis Streams + Pub/Sub 동시 처리
├─ 2.1.1.2 Redis Streams 이벤트 저장 (영구성)
│   ├─ Stream Key: messages:room:{roomId}
│   ├─ Message ID: 자동생성 타임스탬프
│   ├─ Payload: {userId, type, content, mediaUrl, clientTempId, timestamp}
│   └─ Consumer Group: message-processors (배치 저장용)
├─ 2.1.1.3 Redis Pub/Sub 실시간 배포 (즉시성)
│   ├─ 그룹 멤버 조회: SMEMBERS room:{roomId}:members
│   ├─ 각 멤버 큐에 발행: PUBLISH queue:user:{userId} "message"
│   ├─ 온라인 멤버: 즉시 WebSocket 전달
│   └─ 오프라인 멤버: Push Notification Queue에 추가
└─ 2.1.1.4 클라이언트 ACK 방식
    ├─ 임시 ID: clientTempId로 전송 상태 추적
    ├─ 배포 완료: Redis 완료 시점에 pending → sent
    └─ 영구 저장: DB 저장 후 sent → delivered
```

#### **2.1.2 Layer 2: 배치 DB 저장 (Message Workers)**
```
🔄 백그라운드 배치 처리
├─ 2.2.1 Message Consumer Workers (독립 프로세스)
│   ├─ Worker 1-3: 각각 다른 Consumer Group으로 처리
│   ├─ Redis Streams XREADGROUP으로 메시지 Pull
│   ├─ 배치 크기: 50개 메시지 단위 처리
│   └─ 처리 주기: 200ms 간격 폴링
├─ 2.2.2 MongoDB 배치 저장
│   ├─ Collection: messages (roomId로 샤딩)
│   ├─ 스키마: {_id, roomId, userId, type, content, mediaUrl, timestamp, readBy[]}
│   ├─ 인덱스: {roomId: 1, timestamp: -1}, {userId: 1}
│   └─ TTL: 방별 최근 10,000개 메시지 (자동 정리)
├─ 2.2.3 순서 보장 및 중복 제거
│   ├─ 방 단위 Sequential 처리 (roomId 기준 라우팅)
│   ├─ clientTempId 기반 Idempotency 검사
│   ├─ 실패 시: DLQ(failed:messages) 이동
│   └─ 성공 시: XACK로 Redis Streams 확인응답
└─ 2.2.4 실시간 상태 업데이트
    ├─ DB 저장 완료 시: 실제 messageId 생성
    ├─ 상태 업데이트 브로드캐스트: PUBLISH queue:user:{userId} "confirmed"
    ├─ 클라이언트: pending → sent → delivered 상태 전환
    └─ 읽음 확인: 별도 이벤트로 처리
```

#### **2.2.3 Layer 3: 읽음 확인 및 통계 (Analytics Pipeline)**
```
📊 메시지 분석 및 통계
├─ 2.3.1 읽음 확인 시스템
│   ├─ 클라이언트 읽음 이벤트: READ_RECEIPT {messageId, userId, timestamp}
│   ├─ MongoDB 업데이트: messages.readBy 배열에 추가
│   ├─ 실시간 브로드캐스트: 발신자에게 읽음 상태 전송
│   └─ 읽음 카운트 캐시: Redis에 실시간 집계
├─ 2.3.2 방 활동 통계
│   ├─ 메시지 수 집계: 시간대별, 사용자별
│   ├─ 활성 사용자 추적: 일/주/월 단위
│   ├─ 방 인기도 점수: 메시지 빈도 + 참여자 수
│   └─ 데이터 웨어하우스: 배치로 분석 DB 적재
└─ 2.3.3 장애 복구 및 데이터 무결성
    ├─ Redis Persistence: AOF + RDB 하이브리드
    ├─ MongoDB Replica Set: 3-노드 구성
    ├─ Cross-Region Backup: 일일 백업 + WAL 복제
    └─ 메시지 복구: DLQ → 재처리 스케줄러
```

---

## **📁 Phase 3: 마이크로서비스별 구현 명세**

### **3.1 WebSocket 채팅 서버 (Node.js + Socket.io)**

#### **3.1.1 서버 구조 및 초기화**
```typescript
// chat-server/src/app.ts
import express from 'express';
import { Server } from 'socket.io';
import { RedisAdapter } from '@socket.io/redis-adapter';
import { createClient } from 'redis';

class ChatServer {
  private app: express.Application;
  private server: http.Server;
  private io: Server;
  private redisClient: ReturnType<typeof createClient>;
  private pubClient: ReturnType<typeof createClient>;
  private subClient: ReturnType<typeof createClient>;
  private serverId: string;

  constructor() {
    this.serverId = `chat-server-${process.env.SERVER_ID || Math.random()}`;
    this.initializeApp();
    this.initializeRedis();
    this.initializeSocketIO();
    this.initializeRoutes();
  }

  private async initializeRedis() {
    // Redis Cluster 연결
    this.redisClient = createClient({
      socket: { host: 'redis-cluster', port: 6379 },
      password: process.env.REDIS_PASSWORD
    });

    this.pubClient = this.redisClient.duplicate();
    this.subClient = this.redisClient.duplicate();

    await Promise.all([
      this.redisClient.connect(),
      this.pubClient.connect(),
      this.subClient.connect()
    ]);
  }

  private initializeSocketIO() {
    this.io = new Server(this.server, {
      cors: { origin: "*" },
      transports: ['websocket', 'polling'],
      pingTimeout: 60000,
      pingInterval: 25000
    });

    // Redis Adapter for multi-server scaling
    this.io.adapter(createAdapter(this.pubClient, this.subClient));

    this.setupSocketHandlers();
  }

  private setupSocketHandlers() {
    this.io.on('connection', (socket) => {
      console.log(`New connection: ${socket.id}`);

      // JWT 토큰 검증
      socket.on('authenticate', async (token: string) => {
        try {
          const user = await this.verifyJWTToken(token);
          socket.data.user = user;

          // Redis에 연결 정보 저장
          await this.registerConnection(user.id, socket.id);

          // 사용자 개별 큐 구독 시작
          await this.subscribeToUserQueue(user.id, socket);

          socket.emit('authenticated', { success: true, user });
        } catch (error) {
          socket.emit('auth_error', { message: 'Invalid token' });
          socket.disconnect();
        }
      });

      // 방 입장
      socket.on('join_room', async (roomId: string) => {
        if (!socket.data.user) return;

        const hasAccess = await this.verifyRoomAccess(socket.data.user.id, roomId);
        if (!hasAccess) {
          socket.emit('error', { message: 'Room access denied' });
          return;
        }

        await socket.join(`room:${roomId}`);
        await this.addToRoomMembers(roomId, socket.data.user.id);

        socket.emit('joined_room', { roomId });
      });

      // 메시지 전송
      socket.on('send_message', async (data: MessageData) => {
        if (!socket.data.user) return;

        await this.handleMessage(socket.data.user, data);
      });

      // 연결 해제
      socket.on('disconnect', async () => {
        if (socket.data.user) {
          await this.unregisterConnection(socket.data.user.id, socket.id);
        }
      });
    });
  }

  private async handleMessage(user: User, data: MessageData) {
    // 1. Rate Limiting 검사
    const isAllowed = await this.checkRateLimit(user.id, data.roomId);
    if (!isAllowed) {
      throw new Error('Rate limit exceeded');
    }

    // 2. Redis Streams에 메시지 추가 (영구성)
    const messageId = await this.redisClient.xAdd(
      `messages:room:${data.roomId}`,
      '*',
      {
        userId: user.id,
        type: data.type,
        content: data.content,
        mediaUrl: data.mediaUrl || '',
        clientTempId: data.clientTempId,
        timestamp: Date.now().toString()
      }
    );

    // 3. 방 멤버들에게 실시간 전달 (즉시성)
    const members = await this.getRoomMembers(data.roomId);
    const messagePayload = {
      id: messageId,
      roomId: data.roomId,
      user: { id: user.id, nickname: user.nickname },
      type: data.type,
      content: data.content,
      mediaUrl: data.mediaUrl,
      timestamp: new Date().toISOString(),
      clientTempId: data.clientTempId
    };

    // 각 멤버의 개별 큐에 발행
    for (const memberId of members) {
      await this.pubClient.publish(
        `queue:user:${memberId}`,
        JSON.stringify(messagePayload)
      );
    }

    // 4. 오프라인 사용자 감지 및 Push Notification
    const offlineMembers = await this.getOfflineMembers(members);
    if (offlineMembers.length > 0) {
      await this.scheduleNotifications(offlineMembers, messagePayload);
    }
  }

  private async subscribeToUserQueue(userId: string, socket: SocketIO.Socket) {
    const subscriber = this.redisClient.duplicate();
    await subscriber.connect();

    await subscriber.subscribe(`queue:user:${userId}`, (message) => {
      const messageData = JSON.parse(message);
      socket.emit('new_message', messageData);
    });

    // 소켓 연결 해제 시 구독 해제
    socket.on('disconnect', async () => {
      await subscriber.unsubscribe(`queue:user:${userId}`);
      await subscriber.quit();
    });
  }

  private async registerConnection(userId: string, socketId: string) {
    await this.redisClient.hSet(`connections:${userId}`, {
      serverId: this.serverId,
      socketId: socketId,
      connectedAt: Date.now().toString()
    });
  }

  private async verifyJWTToken(token: string): Promise<User> {
    // API Server에 토큰 검증 요청
    const response = await fetch(`${process.env.API_SERVER_URL}/auth/verify`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (!response.ok) {
      throw new Error('Token verification failed');
    }

    return await response.json();
  }
}

export default ChatServer;
```

#### **3.1.2 메시지 배치 처리 워커**
```typescript
// message-worker/src/consumer.ts
import { createClient } from 'redis';
import { MongoClient } from 'mongodb';

class MessageConsumer {
  private redisClient: ReturnType<typeof createClient>;
  private mongoClient: MongoClient;
  private consumerGroup = 'message-processors';
  private consumerName: string;

  constructor() {
    this.consumerName = `worker-${process.env.WORKER_ID || Math.random()}`;
    this.initializeConnections();
  }

  async start() {
    console.log(`Starting message consumer: ${this.consumerName}`);

    // 무한 루프로 메시지 처리
    while (true) {
      try {
        await this.processMessages();
        await this.sleep(200); // 200ms 대기
      } catch (error) {
        console.error('Message processing error:', error);
        await this.sleep(1000); // 에러 시 1초 대기
      }
    }
  }

  private async processMessages() {
    // Redis Streams에서 메시지 읽기
    const streams = await this.redisClient.xReadGroup(
      this.consumerGroup,
      this.consumerName,
      [{ key: 'messages:room:*', id: '>' }],
      { COUNT: 50, BLOCK: 100 }
    );

    if (!streams || streams.length === 0) return;

    const messagesToSave = [];
    const acksToSend = [];

    for (const stream of streams) {
      for (const message of stream.messages) {
        try {
          const messageData = this.parseMessage(message);
          messagesToSave.push(messageData);
          acksToSend.push({ stream: stream.name, id: message.id });
        } catch (error) {
          console.error('Message parsing error:', error);
          // DLQ로 이동
          await this.moveToDeadLetterQueue(stream.name, message);
        }
      }
    }

    // 배치로 MongoDB에 저장
    if (messagesToSave.length > 0) {
      await this.saveBatchToMongoDB(messagesToSave);

      // Redis Streams ACK
      for (const ack of acksToSend) {
        await this.redisClient.xAck(ack.stream, this.consumerGroup, ack.id);
      }

      // 클라이언트에 확인 신호 전송
      await this.broadcastDeliveryConfirmation(messagesToSave);
    }
  }

  private async saveBatchToMongoDB(messages: MessageData[]) {
    const db = this.mongoClient.db('vibechat');
    const collection = db.collection('messages');

    // 중복 제거: clientTempId 기준
    const uniqueMessages = messages.filter((msg, index, self) =>
      index === self.findIndex(m => m.clientTempId === msg.clientTempId)
    );

    try {
      await collection.insertMany(uniqueMessages, { ordered: false });
      console.log(`Saved ${uniqueMessages.length} messages to MongoDB`);
    } catch (error) {
      if (error.code === 11000) { // Duplicate key error
        console.log('Some messages already exist, skipping duplicates');
      } else {
        throw error;
      }
    }
  }

  private async broadcastDeliveryConfirmation(messages: MessageData[]) {
    for (const message of messages) {
      const confirmationPayload = {
        type: 'MESSAGE_CONFIRMED',
        clientTempId: message.clientTempId,
        messageId: message._id,
        timestamp: new Date().toISOString()
      };

      await this.redisClient.publish(
        `queue:user:${message.userId}`,
        JSON.stringify(confirmationPayload)
      );
    }
  }
}
```

### **3.2 REST API 서버 (Spring Boot - 기존 확장)**

#### **3.2.1 JWT 토큰 발급 및 검증 API**
```java
// AuthController.java - 기존에 추가
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/chat-token")
    public ResponseEntity<Map<String, String>> generateChatToken(
            HttpServletRequest request) {

        // 기존 세션에서 사용자 정보 추출
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserPrincipal principal = (UserPrincipal) session.getAttribute("userPrincipal");
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 채팅용 JWT 토큰 생성 (유효기간 24시간)
        String chatToken = jwtTokenProvider.generateChatToken(principal);

        Map<String, String> response = Map.of(
            "chatToken", chatToken,
            "expiresIn", "86400",
            "tokenType", "Bearer"
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyChatToken(
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.substring(7); // "Bearer " 제거
            UserPrincipal principal = jwtTokenProvider.validateChatToken(token);

            Map<String, Object> response = Map.of(
                "id", principal.id(),
                "nickname", principal.nickname(),
                "provider", principal.provider().name(),
                "valid", true
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", e.getMessage()));
        }
    }
}
```

#### **3.2.2 방 멤버십 관리 API**
```java
// RoomMembershipController.java - 신규
@RestController
@RequestMapping("/api/rooms/{roomId}/members")
public class RoomMembershipController {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @PostMapping
    public ResponseEntity<Void> addMember(
            @PathVariable Long roomId,
            @RequestBody Map<String, String> request,
            @AuthUser UserPrincipal principal) {

        String userId = request.get("userId");

        // Redis에 멤버십 정보 추가
        String memberKey = "room:" + roomId + ":members";
        redisTemplate.opsForSet().add(memberKey, userId);

        // 멤버 정보 캐시
        String userKey = "user:" + userId + ":rooms";
        redisTemplate.opsForSet().add(userKey, roomId.toString());

        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Set<String>> getMembers(@PathVariable Long roomId) {
        String memberKey = "room:" + roomId + ":members";
        Set<String> members = redisTemplate.opsForSet().members(memberKey);
        return ResponseEntity.ok(members);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long roomId,
            @PathVariable String userId,
            @AuthUser UserPrincipal principal) {

        String memberKey = "room:" + roomId + ":members";
        redisTemplate.opsForSet().remove(memberKey, userId);

        String userKey = "user:" + userId + ":rooms";
        redisTemplate.opsForSet().remove(userKey, roomId.toString());

        return ResponseEntity.ok().build();
    }
}
```

### **3.3 Push Notification Service (Node.js)**

#### **3.3.1 오프라인 사용자 감지 및 알림**
```typescript
// push-service/src/notifier.ts
import admin from 'firebase-admin';
import { createClient } from 'redis';

class PushNotificationService {
  private redisClient: ReturnType<typeof createClient>;
  private fcm: admin.messaging.Messaging;

  constructor() {
    this.initializeFirebase();
    this.initializeRedis();
    this.startNotificationQueue();
  }

  private async startNotificationQueue() {
    // Redis Queue에서 알림 대상 메시지 처리
    while (true) {
      try {
        const notification = await this.redisClient.blPop(
          'notifications:queue',
          10 // 10초 타임아웃
        );

        if (notification) {
          const data = JSON.parse(notification.element);
          await this.sendNotification(data);
        }
      } catch (error) {
        console.error('Notification processing error:', error);
      }
    }
  }

  async sendNotification(data: NotificationData) {
    const { userId, message, roomId } = data;

    // 사용자의 디바이스 토큰 조회
    const deviceTokens = await this.getUserDeviceTokens(userId);
    if (deviceTokens.length === 0) return;

    const payload = {
      notification: {
        title: `${message.user.nickname}님이 메시지를 보냈습니다`,
        body: this.truncateMessage(message.content),
        icon: '/icon-192x192.png'
      },
      data: {
        roomId: roomId.toString(),
        messageId: message.id,
        type: 'new_message'
      }
    };

    // FCM 멀티캐스트 전송
    const response = await this.fcm.sendMulticast({
      tokens: deviceTokens,
      ...payload
    });

    console.log(`Sent ${response.successCount} notifications to user ${userId}`);

    // 실패한 토큰 정리
    if (response.failureCount > 0) {
      await this.cleanupInvalidTokens(userId, deviceTokens, response.responses);
    }
  }

  private async getUserDeviceTokens(userId: string): Promise<string[]> {
    return this.redisClient.sMembers(`user:${userId}:devices`);
  }

  private truncateMessage(content: string): string {
    return content.length > 100 ? content.substring(0, 100) + '...' : content;
  }
}
```

---

## **🚀 Phase 4: 배포 및 오케스트레이션**

### **4.1 Docker Compose 설정**
```yaml
# docker-compose.yml
version: '3.8'
services:
  # Nginx Load Balancer
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - chat-server-1
      - chat-server-2
      - chat-server-3
      - api-server

  # WebSocket Chat Servers
  chat-server-1:
    build: ./chat-server
    environment:
      - SERVER_ID=1
      - REDIS_URL=redis://redis-master:6379
      - API_SERVER_URL=http://api-server:8000
    ports:
      - "3001:3000"
    depends_on:
      - redis-master
      - redis-slave-1

  chat-server-2:
    build: ./chat-server
    environment:
      - SERVER_ID=2
      - REDIS_URL=redis://redis-master:6379
      - API_SERVER_URL=http://api-server:8000
    ports:
      - "3002:3000"
    depends_on:
      - redis-master

  chat-server-3:
    build: ./chat-server
    environment:
      - SERVER_ID=3
      - REDIS_URL=redis://redis-master:6379
      - API_SERVER_URL=http://api-server:8000
    ports:
      - "3003:3000"
    depends_on:
      - redis-master

  # REST API Server
  api-server:
    build: ./backend
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - REDIS_URL=redis://redis-master:6379
      - DATABASE_URL=postgresql://postgres:password@postgres:5432/vibechat
    ports:
      - "8000:8080"
    depends_on:
      - postgres
      - redis-master

  # Message Workers
  message-worker-1:
    build: ./message-worker
    environment:
      - WORKER_ID=1
      - REDIS_URL=redis://redis-master:6379
      - MONGODB_URL=mongodb://mongo1:27017,mongo2:27017,mongo3:27017/vibechat?replicaSet=rs0
    depends_on:
      - redis-master
      - mongo1

  message-worker-2:
    build: ./message-worker
    environment:
      - WORKER_ID=2
      - REDIS_URL=redis://redis-master:6379
      - MONGODB_URL=mongodb://mongo1:27017,mongo2:27017,mongo3:27017/vibechat?replicaSet=rs0
    depends_on:
      - redis-master
      - mongo1

  # Push Notification Service
  push-service:
    build: ./push-service
    environment:
      - REDIS_URL=redis://redis-master:6379
      - FIREBASE_PROJECT_ID=${FIREBASE_PROJECT_ID}
    volumes:
      - ./firebase-service-account.json:/app/firebase-service-account.json
    depends_on:
      - redis-master

  # Redis Cluster
  redis-master:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes --appendfsync everysec
    volumes:
      - redis-master-data:/data

  redis-slave-1:
    image: redis:7-alpine
    ports:
      - "6380:6379"
    command: redis-server --slaveof redis-master 6379 --appendonly yes
    depends_on:
      - redis-master
    volumes:
      - redis-slave-1-data:/data

  redis-slave-2:
    image: redis:7-alpine
    ports:
      - "6381:6379"
    command: redis-server --slaveof redis-master 6379 --appendonly yes
    depends_on:
      - redis-master
    volumes:
      - redis-slave-2-data:/data

  # MongoDB Replica Set
  mongo1:
    image: mongo:6
    ports:
      - "27017:27017"
    command: mongod --replSet rs0 --oplogSize 128
    volumes:
      - mongo1-data:/data/db

  mongo2:
    image: mongo:6
    ports:
      - "27018:27017"
    command: mongod --replSet rs0 --oplogSize 128
    volumes:
      - mongo2-data:/data/db

  mongo3:
    image: mongo:6
    ports:
      - "27019:27017"
    command: mongod --replSet rs0 --oplogSize 128
    volumes:
      - mongo3-data:/data/db

  # PostgreSQL for API Server
  postgres:
    image: postgres:15
    environment:
      - POSTGRES_DB=vibechat
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=password
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

volumes:
  redis-master-data:
  redis-slave-1-data:
  redis-slave-2-data:
  mongo1-data:
  mongo2-data:
  mongo3-data:
  postgres-data:
```

### **4.2 Nginx 로드 밸런서 설정**
```nginx
# nginx.conf
upstream chat_servers {
    server chat-server-1:3000;
    server chat-server-2:3000;
    server chat-server-3:3000;
}

upstream api_servers {
    server api-server:8000;
}

server {
    listen 80;
    server_name localhost;

    # WebSocket Chat Servers
    location /socket.io/ {
        proxy_pass http://chat_servers;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket specific
        proxy_read_timeout 86400;
        proxy_send_timeout 86400;
        proxy_cache_bypass $http_upgrade;
    }

    # REST API Server
    location /api/ {
        proxy_pass http://api_servers;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Frontend Static Files
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
}
```

---

## **🧪 Phase 5: 테스트 및 모니터링**

### **5.1 통합 테스트 시나리오**
```typescript
// tests/integration/chat-flow.test.ts
describe('마이크로서비스 채팅 플로우', () => {
  it('1:1 메시지 전달 테스트', async () => {
    // 1. 두 사용자 WebSocket 연결
    const userA = await connectUser('user-a', 'token-a');
    const userB = await connectUser('user-b', 'token-b');

    // 2. User A가 User B에게 메시지 전송
    const message = { to: 'user-b', content: 'Hello World!' };
    userA.send('send_message', message);

    // 3. User B가 실시간으로 메시지 수신 확인
    const received = await userB.waitForMessage('new_message');
    expect(received.content).toBe('Hello World!');
    expect(received.from).toBe('user-a');

    // 4. MongoDB에 메시지 저장 확인 (최대 5초 대기)
    await waitForCondition(async () => {
      const savedMessage = await mongoClient
        .db('vibechat')
        .collection('messages')
        .findOne({ content: 'Hello World!' });
      return savedMessage !== null;
    }, 5000);
  });

  it('그룹 채팅 메시지 배포 테스트', async () => {
    // 1. 3명 사용자가 같은 방에 입장
    const users = await Promise.all([
      connectUserToRoom('user-a', 'room-123'),
      connectUserToRoom('user-b', 'room-123'),
      connectUserToRoom('user-c', 'room-123')
    ]);

    // 2. User A가 그룹 메시지 전송
    users[0].send('send_message', {
      roomId: 'room-123',
      content: 'Hello Group!'
    });

    // 3. 다른 모든 멤버가 메시지 수신 확인
    const receivedMessages = await Promise.all([
      users[1].waitForMessage('new_message'),
      users[2].waitForMessage('new_message')
    ]);

    receivedMessages.forEach(msg => {
      expect(msg.content).toBe('Hello Group!');
      expect(msg.roomId).toBe('room-123');
    });
  });

  it('서버 장애 시 자동 재연결 테스트', async () => {
    // 1. 사용자 연결
    const user = await connectUser('user-a', 'token-a');

    // 2. 연결된 Chat Server 강제 종료
    await killChatServer(user.connectedServerId);

    // 3. 자동 재연결 확인 (10초 이내)
    await waitForCondition(async () => {
      return user.isConnected();
    }, 10000);

    // 4. 재연결 후 메시지 송수신 정상 동작 확인
    user.send('send_message', { content: 'After reconnection' });
    // 검증 로직...
  });
});
```

### **5.2 성능 및 부하 테스트**
```typescript
// tests/load/chat-performance.test.ts
describe('채팅 서버 성능 테스트', () => {
  it('1000명 동시 접속 테스트', async () => {
    const connections = [];

    // 1000개 동시 연결 생성
    for (let i = 0; i < 1000; i++) {
      const user = await connectUser(`user-${i}`, `token-${i}`);
      connections.push(user);
    }

    // 모든 연결이 성공적으로 수립되었는지 확인
    expect(connections.length).toBe(1000);
    connections.forEach(conn => {
      expect(conn.isConnected()).toBe(true);
    });

    // 메모리 사용량 확인
    const memoryUsage = await getChatServerMemoryUsage();
    expect(memoryUsage).toBeLessThan(512 * 1024 * 1024); // 512MB 이하
  });

  it('초당 10,000 메시지 처리 테스트', async () => {
    const users = await createTestUsers(100); // 100명 사용자
    const messagesPerSecond = 10000;
    const testDuration = 10; // 10초

    const startTime = Date.now();
    const messagePromises = [];

    // 10초 동안 초당 10,000개 메시지 전송
    for (let second = 0; second < testDuration; second++) {
      setTimeout(() => {
        for (let i = 0; i < messagesPerSecond; i++) {
          const randomUser = users[Math.floor(Math.random() * users.length)];
          const promise = randomUser.send('send_message', {
            content: `Message ${i} at second ${second}`
          });
          messagePromises.push(promise);
        }
      }, second * 1000);
    }

    await Promise.all(messagePromises);
    const endTime = Date.now();
    const actualDuration = (endTime - startTime) / 1000;

    // 처리 시간이 15초를 넘지 않는지 확인 (여유 있게)
    expect(actualDuration).toBeLessThan(15);

    // 메시지 손실 없이 모두 저장되었는지 확인
    await waitForCondition(async () => {
      const savedCount = await mongoClient
        .db('vibechat')
        .collection('messages')
        .countDocuments({
          timestamp: { $gte: new Date(startTime) }
        });
      return savedCount === messagesPerSecond * testDuration;
    }, 30000); // 30초 대기
  });
});
```

### **5.3 모니터링 및 알람 설정**
```yaml
# monitoring/prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'chat-servers'
    static_configs:
      - targets:
        - 'chat-server-1:3000'
        - 'chat-server-2:3000'
        - 'chat-server-3:3000'
    metrics_path: '/metrics'

  - job_name: 'api-server'
    static_configs:
      - targets: ['api-server:8000']
    metrics_path: '/actuator/prometheus'

  - job_name: 'redis'
    static_configs:
      - targets: ['redis-master:6379']

  - job_name: 'mongodb'
    static_configs:
      - targets: ['mongo1:27017']

rule_files:
  - "alert_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']
```

```yaml
# monitoring/alert_rules.yml
groups:
  - name: chat_alerts
    rules:
      - alert: HighMessageLatency
        expr: histogram_quantile(0.95, rate(message_processing_duration_seconds_bucket[5m])) > 1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "메시지 처리 지연 발생"
          description: "p95 메시지 처리 시간이 1초를 초과했습니다."

      - alert: ChatServerDown
        expr: up{job="chat-servers"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "채팅 서버 다운"
          description: "채팅 서버 {{ $labels.instance }}가 다운되었습니다."

      - alert: RedisConnectionFailure
        expr: redis_connected_clients < 1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Redis 연결 실패"
          description: "Redis 서버에 연결된 클라이언트가 없습니다."

      - alert: MessageQueueBacklog
        expr: redis_stream_length > 1000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "메시지 큐 적체"
          description: "Redis Stream에 처리되지 않은 메시지가 1000개 이상 적체되었습니다."
```

---

## **📋 구현 순서 및 마일스톤**

### **Week 1: 기반 인프라 구축**
- [ ] Docker Compose 환경 구성
- [ ] Redis Cluster 설정
- [ ] MongoDB Replica Set 구성
- [ ] Nginx 로드 밸런서 설정

### **Week 2: WebSocket 채팅 서버 개발**
- [ ] Node.js + Socket.io 기본 구조
- [ ] JWT 토큰 인증 시스템
- [ ] Redis Pub/Sub 메시지 라우팅
- [ ] 1:1 메시지 전달 구현

### **Week 3: REST API 서버 확장**
- [ ] JWT 토큰 발급/검증 API
- [ ] 방 멤버십 관리 API
- [ ] Redis 캐시 통합
- [ ] 기존 Spring Boot 앱 확장

### **Week 4: 메시지 배치 처리**
- [ ] Redis Streams 구현
- [ ] Message Consumer Workers
- [ ] MongoDB 배치 저장
- [ ] 메시지 순서 보장 및 중복 제거

### **Week 5: Push Notification 서비스**
- [ ] 오프라인 사용자 감지
- [ ] FCM/APNs 통합
- [ ] 알림 큐 처리
- [ ] 디바이스 토큰 관리

### **Week 6: 프론트엔드 통합**
- [ ] WebSocket 클라이언트 리팩토링
- [ ] JWT 토큰 기반 인증 전환
- [ ] 실시간 상태 동기화
- [ ] 오프라인 모드 지원

### **Week 7-8: 테스트 및 최적화**
- [ ] 통합 테스트 구현
- [ ] 부하 테스트 및 성능 최적화
- [ ] 모니터링 시스템 구축
- [ ] 장애 복구 테스트

---

## **✅ 성공 기준 및 검증 포인트**

### **기능 검증**
- [ ] 1:1 메시지 전달 성공률 99.9%
- [ ] 그룹 메시지 모든 멤버 전달 100%
- [ ] 오프라인 사용자 Push 알림 95%
- [ ] 메시지 순서 보장 100%

### **성능 검증**
- [ ] 메시지 지연 p95 < 1초
- [ ] 동시 1,000명 접속 안정성
- [ ] 초당 10,000 메시지 처리
- [ ] 서버 재시작 후 30초 내 복구

### **확장성 검증**
- [ ] Chat Server 수평 확장 테스트
- [ ] Redis Cluster 샤딩 동작
- [ ] MongoDB 자동 샤딩 확인
- [ ] 로드 밸런서 트래픽 분산

---

**이제 이 마이크로서비스 아키텍처를 단계별로 구현하여 확장 가능하고 고성능의 실시간 채팅 시스템을 구축할 수 있습니다!**