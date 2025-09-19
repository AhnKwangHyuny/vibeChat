# 🚀 VibeChat - 그룹챗 기술 아키텍처 문서

## 📊 현재 시스템 분석 및 개선 로드맵

### **현재 구현 상태**
- **SimpleBroker**: 메모리 기반 (스케일링 한계)
- **즉시 저장**: 동기식 DB 저장 + 브로드캐스트
- **Redis**: Presence/Typing만 활용
- **파일**: 로컬 파일시스템 (S3 미적용)

---

## 🎯 **카카오톡 스타일 메시징 시스템 - 완전 아키텍처**

### **목표: p95 메시지 지연 ≤ 1초, 100명 동시 접속, 무손실 메시지 전달**

---

## **📡 Phase 1: WebSocket 연결 아키텍처**

### **1.1 방 개설 시 WebSocket 브로커 초기화**
```
🏗️ 방 생성 완료 시점
├─ 1.1.1 ChatRoom Entity 저장 완료 → roomId 확정
├─ 1.1.2 Redis 토픽 사전 생성
│   ├─ presence:room:{roomId}:sessions (SET)
│   ├─ message:queue:room:{roomId} (LIST) ← 새로 추가
│   ├─ typing:room:{roomId} (SET with TTL)
│   └─ nickname:room:{roomId} (SET)
├─ 1.1.3 STOMP 브로커 토픽 사전 등록
│   ├─ /topic/rooms/{roomId}/messages
│   ├─ /topic/rooms/{roomId}/typing
│   ├─ /topic/rooms/{roomId}/presence
│   └─ /topic/rooms/{roomId}/status ← 새로 추가 (방 상태)
└─ 1.1.4 메시지 처리 워커 스레드 할당
    ├─ MessageConsumerWorker-{roomId} 스레드 생성
    ├─ Redis Stream: messages:{roomId} 컨슈머 그룹 생성
    └─ Async Batch Writer 스케줄러 등록
```

### **1.2 클라이언트 WebSocket 연결 라이프사이클**
```
🔌 연결 수립 (Handshake)
├─ 1.2.1 HTTP → WebSocket 업그레이드 요청
│   ├─ Origin 검증: SecurityConfig 화이트리스트
│   ├─ Cookie 기반 세션 검증 (JSESSIONID)
│   └─ 성공 시: STOMP/WebSocket 프로토콜 전환
├─ 1.2.2 STOMP CONNECT 프레임 처리
│   ├─ StompEventListener.handleConnect() 호출
│   ├─ 세션 속성 추출: userId, nickname, roomId
│   ├─ 세션 상태: Redis Hash 저장
│   │   └─ presence:session:{stompSessionId} = {userId, roomId, nickname, connectedAt}
│   └─ Heartbeat 설정: client ↔ server 10초 간격
└─ 1.2.3 토픽 구독 (Subscribe)
    ├─ /topic/rooms/{roomId}/messages
    ├─ /topic/rooms/{roomId}/typing
    ├─ /topic/rooms/{roomId}/presence
    └─ 구독 성공 시: PresenceService.userConnected() 호출
```

---

## **💾 Phase 2: 메시지 저장 아키텍처 (3-Layer)**

### **2.1 Layer 1: 실시간 메시지 큐 (Redis Streams)**
```
⚡ 메시지 수신 즉시 처리
├─ 2.1.1 ChatWsController.sendMessage() 진입점
│   ├─ Rate Limiting 검사 (Bucket4j + Redis)
│   ├─ 기본 검증: 사용자 인증, 방 권한, 메시지 형식
│   └─ 통과 시: Redis Stream 즉시 추가 (논블로킹)
├─ 2.1.2 Redis Streams 구조
│   ├─ Stream Key: messages:{roomId}
│   ├─ Consumer Group: chat-processors
│   ├─ Message ID: 자동생성 (타임스탬프 기반)
│   └─ Payload: {userId, type, content, mediaUrl, clientTempId, timestamp}
├─ 2.1.3 즉시 브로드캐스트 (Pre-DB)
│   ├─ 임시 메시지 ID: "pending_{clientTempId}"
│   ├─ Redis 내용 기반으로 WebSocketMessageResponse 생성
│   ├─ STOMP 브로드캐스트: /topic/rooms/{roomId}/messages
│   └─ 클라이언트: pending → confirmed 상태 전환 대기
└─ 2.1.4 ACK 방식: 최대 1초 내 DB 저장 완료 신호
```

### **2.2 Layer 2: 배치 DB 저장 (Async Workers)**
```
🔄 백그라운드 배치 처리
├─ 2.2.1 MessageConsumerWorker 설계
│   ├─ @Async 메서드로 비동기 실행
│   ├─ Redis Streams XREADGROUP 명령으로 메시지 Pull
│   ├─ 배치 크기: 10-50개 메시지 단위로 처리
│   └─ 처리 주기: 100ms 간격 폴링 (튜닝 가능)
├─ 2.2.2 배치 저장 로직
│   ├─ JPA Batch Insert: @BatchSize(50) 어노테이션 활용
│   ├─ Transaction 범위: 배치 단위로 커밋
│   ├─ 실패 시: DLQ(Dead Letter Queue)로 이동
│   └─ 성공 시: Redis Stream XACK로 확인응답
├─ 2.2.3 메시지 순서 보장
│   ├─ 방 단위 Sequential 처리 (roomId 기준 샤딩)
│   ├─ Redis Streams 특성상 순서 자동 보장
│   ├─ DB 저장 후: createdAt = Redis timestamp 매핑
│   └─ 클라이언트: 임시 ID → 실제 DB ID 교체
└─ 2.2.4 확인 브로드캐스트
    ├─ DB 저장 완료 시: message.id 포함한 UPDATE 브로드캐스트
    ├─ /topic/rooms/{roomId}/status 토픽 활용
    ├─ Payload: {action: "CONFIRMED", clientTempId, messageId}
    └─ 클라이언트: pending 상태 제거
```

### **2.3 Layer 3: 영속성 및 복구 (MySQL + Redis)**
```
💾 데이터 보장 계층
├─ 2.3.1 MySQL 메시지 테이블 설계
│   ├─ Primary Key: Auto Increment (성능 최적화)
│   ├─ 복합 인덱스: (room_id, created_at DESC) 커버링
│   ├─ 파티셔닝: 월별 테이블 분할 (선택적)
│   └─ 보관 정책: 방별 최근 1,000개 (스케줄러 트림)
├─ 2.3.2 Redis 백업 전략
│   ├─ Redis Persistence: AOF + RDB 하이브리드
│   ├─ Streams TTL: 24시간 (장애 복구용)
│   ├─ 실패 메시지 복구: DLQ → 재처리 스케줄러
│   └─ 캐시 워밍: 최근 30개 메시지 Redis 유지
└─ 2.3.3 일관성 보장
    ├─ At-Least-Once 전달 보장 (Redis Streams 특성)
    ├─ 중복 제거: clientTempId 기반 Idempotency
    ├─ 장애 복구: 미완료 메시지 재처리 큐
    └─ 모니터링: 메시지 지연 시간 추적
```

---

## **📁 Phase 3: 미디어 파일 처리 워크플로우**

### **3.1 파일 업로드 → S3 → 메시지 전송 파이프라인**
```
📤 미디어 업로드 플로우
├─ 3.1.1 클라이언트 업로드 요청
│   ├─ POST /api/upload/media (multipart/form-data)
│   ├─ 사전 검증: 파일 크기(20MB), 형식(image/gif/video), 길이(10초)
│   ├─ 임시 저장: /tmp/upload/{uuid} (서버 로컬)
│   └─ 응답: { uploadId, status: "PROCESSING" }
├─ 3.1.2 백그라운드 S3 업로드 (비동기)
│   ├─ @Async UploadService.processMediaFile() 호출
│   ├─ 미디어 타입별 처리:
│   │   ├─ 이미지: Thumbnailator 리사이즈 (512px 썸네일)
│   │   ├─ GIF: 원본 유지 + 첫 프레임 썸네일
│   │   └─ 영상: FFmpeg 포스터 생성 (1초 지점)
│   ├─ S3 Upload:
│   │   ├─ 원본: s3://vibechat-media/rooms/{roomId}/{messageId}.{ext}
│   │   ├─ 썸네일: s3://vibechat-media/rooms/{roomId}/{messageId}_thumb.{ext}
│   │   └─ ACL: public-read (CloudFront 배포용)
│   └─ Redis 상태 업데이트: upload:{uploadId} = {status: "COMPLETED", urls: {...}}
├─ 3.1.3 업로드 완료 확인 (폴링 or SSE)
│   ├─ 클라이언트: GET /api/upload/{uploadId}/status 폴링
│   ├─ 성공 응답: { status: "COMPLETED", mediaUrl, thumbUrl, durationSec }
│   └─ 실패 응답: { status: "FAILED", error: "Processing failed" }
└─ 3.1.4 메시지 전송
    ├─ SendMessagePayload 구성: {type: "IMAGE", mediaUrl, thumbUrl}
    ├─ WebSocket 전송: /app/rooms/{roomId}/send
    └─ 위의 2.1~2.2 플로우와 동일하게 처리
```

### **3.2 CloudFront + S3 배포 최적화**
```
🌐 CDN 배포 전략
├─ 3.2.1 S3 버킷 구조
│   ├─ /rooms/{roomId}/{timestamp}_{messageId}.{ext}
│   ├─ /thumbnails/{roomId}/{timestamp}_{messageId}_thumb.{ext}
│   └─ /temp/{uploadId}.{ext} (업로드 중 임시 경로)
├─ 3.2.2 CloudFront 설정
│   ├─ Origin: S3 버킷 (vibechat-media.s3.amazonaws.com)
│   ├─ 캐시 정책: 이미지/영상 24시간, 썸네일 1주일
│   ├─ 압축: Gzip 자동 적용
│   └─ 지역별 Edge Location 활용
└─ 3.2.3 클라이언트 최적화
    ├─ 썸네일 우선 로딩 (lazy loading)
    ├─ 원본 이미지: 클릭 시 모달로 로드
    ├─ 영상: 포스터 표시 → 클릭 시 재생
    └─ 프리로딩: 다음/이전 메시지 미디어 대기 로드
```

---

## **⚙️ Phase 4: 메시지 큐 및 배치 처리 세부 설계**

### **4.1 Redis Streams 기반 메시지 큐 시스템**
```
🏭 Producer-Consumer 패턴
├─ 4.1.1 Producer (ChatWsController)
│   ├─ XADD messages:{roomId} * 명령 사용
│   ├─ 메시지 구조: {user:123, type:TEXT, content:"hello", temp:"uuid", ts:1640995200}
│   ├─ 최대 큐 크기: MAXLEN ~ 1000 (메모리 보호)
│   └─ 초당 처리량: 방당 최대 100 메시지
├─ 4.1.2 Consumer Group 설계
│   ├─ 그룹명: chat-processors-{roomId}
│   ├─ 컨슈머 인스턴스: 방당 1개 (순서 보장)
│   ├─ XREADGROUP 블로킹 모드: 100ms timeout
│   └─ Acknowledgment: 배치 처리 완료 후 XACK
├─ 4.1.3 배치 처리 알고리즘
│   ├─ 수집 윈도우: 100ms 또는 10개 메시지 (선도달 기준)
│   ├─ JPA Batch Insert: repository.saveAll() 활용
│   ├─ 트랜잭션 격리: REPEATABLE_READ 레벨
│   └─ 실패 처리: 3회 재시도 → DLQ 이동
└─ 4.1.4 백프레셔 제어
    ├─ 큐 길이 모니터링: XLEN messages:{roomId}
    ├─ 임계값 초과 시: Rate Limiting 강화
    ├─ Consumer Lag 추적: XPENDING 명령 활용
    └─ 알림: 지연 5초 초과 시 관리자 알람
```

### **4.2 장애 복구 및 메시지 보장**
```
🛡️ 무손실 메시지 처리
├─ 4.2.1 At-Least-Once 전달 보장
│   ├─ Redis Streams PEL(Pending Entry List) 활용
│   ├─ Consumer 장애 시: XCLAIM으로 메시지 재할당
│   ├─ XAUTOCLAIM 자동 복구: 30초 timeout
│   └─ 중복 제거: DB의 client_temp_id UNIQUE 제약
├─ 4.2.2 Dead Letter Queue 처리
│   ├─ DLQ: failed:messages:{roomId} (별도 Stream)
│   ├─ 이동 조건: 3회 재시도 실패 또는 크리티컬 에러
│   ├─ 수동 복구: 관리자 도구로 DLQ → 메인 큐 재투입
│   └─ 모니터링: DLQ 크기 추적 및 알람
├─ 4.2.3 분할된 브레인(Split-Brain) 방지
│   ├─ Redis Sentinel 또는 Cluster 모드 사용
│   ├─ 마스터 장애 시: 자동 페일오버
│   ├─ 컨슈머 리더 선출: Redis 분산 락 활용
│   └─ 중복 처리 방지: DB 레벨 유니크 제약 + 애플리케이션 검증
└─ 4.2.4 메시지 순서 보장
    ├─ 방 단위 Sequential 처리 (동시성 제한)
    ├─ Redis Streams 자체 순서 보장 특성 활용
    ├─ 배치 내 정렬: timestamp 기준 ASC 정렬
    └─ 클라이언트: 순서 역전 방지 버퍼링
```

---

## **🚀 Phase 5: 스케일링 및 성능 최적화**

### **5.1 수평 확장 아키텍처**
```
📈 멀티 인스턴스 배포
├─ 5.1.1 로드 밸런서 (Nginx/ALB) 설정
│   ├─ Sticky Session: WebSocket 연결 유지
│   ├─ 라우팅 전략: roomId hash 기반 분산
│   ├─ Health Check: /actuator/health 엔드포인트
│   └─ Circuit Breaker: 인스턴스 장애 시 자동 제외
├─ 5.1.2 Redis Cluster 도입
│   ├─ 샤딩: roomId 기준으로 슬롯 분산
│   ├─ 복제: Master-Slave 구성 (3M-3S)
│   ├─ Streams 분산: 방별로 다른 노드에 배치
│   └─ Client-side Sharding: Lettuce 클러스터 지원
├─ 5.1.3 DB 읽기 복제
│   ├─ Master: 쓰기 전용 (메시지 저장)
│   ├─ Slave: 읽기 전용 (메시지 조회, 통계)
│   ├─ Connection Pool: HikariCP 최적화
│   └─ Read/Write 분리: @Transactional(readOnly=true)
└─ 5.1.4 메시지 브로커 외부화 (고도화)
    ├─ RabbitMQ 또는 Apache Kafka 도입 검토
    ├─ STOMP over RabbitMQ: External Broker 설정
    ├─ 토픽 파티셔닝: 방 단위 자동 분산
    └─ 컨슈머 그룹: 자동 리밸런싱 지원
```

### **5.2 성능 모니터링 및 튜닝**
```
📊 실시간 성능 추적
├─ 5.2.1 메트릭 수집 (Micrometer + Prometheus)
│   ├─ 메시지 처리 지연: Timer("message.processing.duration")
│   ├─ WebSocket 연결 수: Gauge("websocket.connections.active")
│   ├─ Redis 큐 길이: Gauge("redis.queue.length")
│   └─ DB 커넥션 풀: HikariCP 메트릭 자동 수집
├─ 5.2.2 분산 추적 (Zipkin/Jaeger)
│   ├─ 요청 ID: MDC 기반 추적 (CorrelationIdFilter)
│   ├─ 스팬: WebSocket → Redis → DB → Broadcast
│   ├─ 지연 구간 식별: 각 스팬별 소요 시간 측정
│   └─ 에러 추적: 실패 지점 및 스택 트레이스
├─ 5.2.3 알람 임계값 설정
│   ├─ 메시지 지연: p95 > 1초 시 슬랙 알림
│   ├─ 큐 적체: 길이 > 100개 시 에스컬레이션
│   ├─ DB 응답: 평균 > 500ms 시 성능 경고
│   └─ 에러율: 분당 > 10건 시 크리티컬 알람
└─ 5.2.4 자동 스케일링 트리거
    ├─ CPU 사용률: 평균 70% 초과 시 Pod 증설
    ├─ 메모리 사용률: 85% 초과 시 스케일 아웃
    ├─ 큐 지연: 지속적 적체 시 컨슈머 인스턴스 추가
    └─ 동접자 수: 방당 100명 초과 시 브로커 분산
```

---

## **📋 현재 프로젝트 구현 상태 & 유저 플로우 기반 구현 순서**

### **✅ 완료된 기능들**
```
🏗️ 백엔드 인프라
✅ Spring Boot 3.3 + WebSocket/STOMP 설정
✅ Redis 세션 관리 및 Presence 서비스
✅ 기본 인증 시스템 (게스트/구글)
✅ 방 CRUD API (생성/조회/입장)
✅ 메시지 기본 저장/브로드캐스트
✅ Rate Limiting (Bucket4j)
✅ 파일 업로드 컨트롤러 기본 구조

🎨 프론트엔드 UI/UX
✅ React 18 + TypeScript + Redux Toolkit
✅ 완성된 컴포넌트 라이브러리 (ui/, demo/, layout/)
✅ 페이지 라우팅 (Home, Room, CreateRoom)
✅ WebSocket 클라이언트 (자동 재연결)
✅ 인증 훅 (useSessionAuth, useSupabaseAuth)
✅ 기본 메시지 UI 컴포넌트들
```

### **⚠️ 부분 완료 (보완 필요)**
```
🔄 메시지 시스템
⚠️ 메시지 저장: 동기식만 구현 (배치 처리 필요)
⚠️ 파일 업로드: 로컬 저장 (S3 워크플로우 필요)
⚠️ 실시간 기능: 기본 구현 (최적화 필요)
⚠️ 에러 처리: 기본 수준 (고도화 필요)
```

### **❌ 미구현 (신규 개발 필요)**
```
🚀 고도화 기능
❌ Redis Streams 메시지 큐 시스템
❌ S3 + CloudFront 미디어 처리
❌ 배치 메시지 저장 워커
❌ 메시지 순서 보장 및 무결성
❌ 장애 복구 및 DLQ 처리
❌ 성능 모니터링 및 메트릭
```

---

## **🎮 유저 플로우 기반 구현 순서**

### **👤 유저 사용 시나리오 순서**
```
1. 브라우저에서 vibeChat 접속
2. 닉네임 입력하여 게스트 로그인
3. 홈에서 방 검색 또는 새 방 생성
4. 방 클릭하여 입장
5. 실시간 메시지 송수신
6. 이미지/영상 업로드하여 공유
7. 다른 사용자와 타이핑 상태 확인
8. 방 나가기
```

### **🔧 구현 우선순위 (유저 경험 순서대로)**

---

## **Phase 1: 기본 채팅 플로우 안정화 (1-2일)**
*"유저가 방에 들어가서 텍스트 메시지를 주고받을 수 있게"*

### **1-1. 방 입장 플로우 완성 ✅→🔧**
```
현재 상태: ✅ 기본 구현됨
개선 필요:
├─ 🔧 방 입장 시 nickname 중복 체크 강화
├─ 🔧 비공개방 초대코드 검증 로직 보완
├─ 🔧 입장 실패 시 명확한 에러 메시지
└─ 🔧 WebSocket 연결 전 권한 재검증

테스트 시나리오:
1. 공개방 정상 입장
2. 비공개방 올바른 초대코드로 입장
3. 잘못된 초대코드로 입장 실패
4. 닉네임 중복 시 대체 제안
```

### **1-2. WebSocket 연결 안정화 ✅→🔧**
```
현재 상태: ✅ stompClient 구현됨
개선 필요:
├─ 🔧 연결 실패 시 사용자 친화적 안내
├─ 🔧 세션 만료 시 자동 재인증 유도
├─ 🔧 네트워크 끊김 복구 시 메시지 동기화
└─ 🔧 Heartbeat 실패 시 즉시 재연결

테스트 시나리오:
1. 정상 연결 및 토픽 구독
2. 네트워크 끊김 후 재연결
3. 세션 만료 후 재인증
4. 서버 재시작 후 자동 복구
```

### **1-3. 텍스트 메시지 송수신 완성 ⚠️→✅**
```
현재 상태: ⚠️ 기본 구현, 개선 필요
구현 목표:
├─ 🔧 메시지 전송 중 로딩 상태 표시
├─ 🔧 전송 실패 시 재시도 버튼
├─ 🔧 pending→sent→delivered 상태 관리
└─ 🔧 메시지 순서 보장 (clientTempId 활용)

테스트 시나리오:
1. 일반 텍스트 메시지 송수신
2. 긴 메시지 (2000자) 송수신
3. 네트워크 지연 시 pending 상태
4. 전송 실패 후 재시도
```

---

## **Phase 2: 실시간 기능 완성 (2-3일)**
*"타이핑 표시, 온라인 상태 등 실시간 인터랙션"*

### **2-1. Presence 시스템 고도화 ⚠️→✅**
```
현재 상태: ⚠️ 기본 구현됨
구현 목표:
├─ 🔧 입장/퇴장 즉시 브로드캐스트 (1초 이내)
├─ 🔧 사용자 리스트 실시간 업데이트
├─ 🔧 네트워크 끊김 시 "오프라인" 표시
└─ 🔧 중복 연결 방지 및 이전 세션 정리

테스트 시나리오:
1. 두 브라우저에서 동시 입장
2. 한 쪽 브라우저 종료 시 즉시 반영
3. 새로고침 시 중복 없이 재연결
4. 온라인 카운트 정확성 검증
```

### **2-2. 타이핑 인디케이터 구현 ⚠️→✅**
```
현재 상태: ⚠️ 기본 구현됨
구현 목표:
├─ 🔧 타이핑 시작/종료 즉시 반영 (500ms 이내)
├─ 🔧 3초 자동 타이핑 종료 (서버 TTL)
├─ 🔧 동시 타이핑자 목록 표시
└─ 🔧 자신의 타이핑 제외하고 표시

테스트 시나리오:
1. 입력 시작 시 "typing..." 표시
2. 입력 중단 3초 후 자동 제거
3. 여러 명 동시 타이핑 시 목록 표시
4. 메시지 전송 시 즉시 타이핑 제거
```

### **2-3. 메시지 상태 표시 시스템 ❌→✅**
```
현재 상태: ❌ 미구현
구현 목표:
├─ 🆕 전송 중: 로딩 스피너
├─ 🆕 전송 완료: 회색 체크마크
├─ 🆕 읽음 표시: 파란 체크마크 (선택적)
└─ 🆕 전송 실패: 빨간 경고 아이콘 + 재시도

테스트 시나리오:
1. 메시지 전송 시 로딩→완료 순서
2. 전송 실패 시 재시도 버튼
3. 상대방 읽음 시 파란 체크 (선택적)
4. 오프라인 시 대기 상태 표시
```

---

## **Phase 3: 미디어 파일 처리 (3-4일)**
*"이미지, 영상 업로드 및 S3 연동"*

### **3-1. 파일 업로드 UI/UX 완성 ⚠️→✅**
```
현재 상태: ⚠️ 기본 파일 선택만 구현
구현 목표:
├─ 🔧 드래그 앤 드롭 업로드 영역
├─ 🔧 업로드 전 미리보기 (이미지/영상)
├─ 🔧 업로드 진행률 표시 (0-100%)
└─ 🔧 파일 형식/크기 검증 (클라이언트)

테스트 시나리오:
1. 이미지 드래그 앤 드롭 업로드
2. 10초 초과 영상 업로드 차단
3. 20MB 초과 파일 업로드 차단
4. 업로드 중 취소 기능
```

### **3-2. S3 업로드 워크플로우 구현 ❌→✅**
```
현재 상태: ❌ 로컬 파일시스템만 사용
구현 목표:
├─ 🆕 S3 Pre-signed URL 생성 API
├─ 🆕 클라이언트 직접 S3 업로드
├─ 🆕 업로드 완료 후 백엔드 알림
└─ 🆕 CloudFront CDN URL 반환

테스트 시나리오:
1. 이미지 S3 업로드 및 CDN URL 생성
2. 대용량 파일 멀티파트 업로드
3. 업로드 실패 시 재시도 메커니즘
4. CDN 캐시 적용 확인
```

### **3-3. 미디어 메시지 표시 최적화 ❌→✅**
```
현재 상태: ❌ 기본 img 태그만 사용
구현 목표:
├─ 🆕 썸네일 우선 로딩 (lazy loading)
├─ 🆕 이미지 클릭 시 전체화면 모달
├─ 🆕 영상 포스터 표시 + 재생 버튼
└─ 🆕 GIF 자동 재생 옵션

테스트 시나리오:
1. 썸네일 로딩 후 원본 이미지 로드
2. 이미지 클릭 시 모달 확대보기
3. 영상 포스터 클릭 시 재생
4. 네트워크 느림 시 점진적 로딩
```

---

## **Phase 4: 메시지 큐 시스템 고도화 (4-5일)**
*"Redis Streams + 배치 처리로 성능 최적화"*

### **4-1. Redis Streams 메시지 큐 도입 ❌→✅**
```
현재 상태: ❌ 동기식 DB 저장만 사용
구현 목표:
├─ 🆕 메시지 수신 즉시 Redis Streams 추가
├─ 🆕 비동기 배치 처리로 DB 저장
├─ 🆕 At-Least-Once 전달 보장
└─ 🆕 Consumer Group으로 순서 보장

테스트 시나리오:
1. 초당 50개 메시지 처리 성능
2. 서버 재시작 시 누락 메시지 복구
3. 배치 처리 실패 시 재시도
4. 메시지 순서 정확성 검증
```

### **4-2. 메시지 무결성 보장 시스템 ❌→✅**
```
현재 상태: ❌ 기본 예외 처리만 구현
구현 목표:
├─ 🆕 clientTempId 기반 중복 제거
├─ 🆕 Dead Letter Queue (DLQ) 처리
├─ 🆕 메시지 확인응답 (ACK) 시스템
└─ 🆕 장애 복구 시 메시지 동기화

테스트 시나리오:
1. 네트워크 끊김 후 메시지 복구
2. 중복 전송 시 자동 중복 제거
3. DLQ 메시지 수동 복구
4. 대량 메시지 부하 테스트
```

---

## **Phase 5: 성능 최적화 & 모니터링 (1-2일)**
*"실제 서비스 수준의 안정성 확보"*

### **5-1. 성능 모니터링 시스템 ❌→✅**
```
현재 상태: ❌ 기본 로깅만 구현
구현 목표:
├─ 🆕 메시지 지연 시간 추적 (p95 < 1초)
├─ 🆕 WebSocket 연결 상태 모니터링
├─ 🆕 Redis/DB 성능 메트릭 수집
└─ 🆕 에러율 및 알람 시스템

테스트 시나리오:
1. 100명 동시 접속 부하 테스트
2. 메시지 지연 시간 측정
3. 메모리/CPU 사용률 모니터링
4. 장애 발생 시 알람 동작
```

### **5-2. 클라이언트 최적화 ❌→✅**
```
현재 상태: ❌ 기본 React 렌더링
구현 목표:
├─ 🆕 메시지 가상 스크롤 (1000개+ 메시지)
├─ 🆕 이미지 지연 로딩 최적화
├─ 🆕 Redux 상태 정규화
└─ 🆕 메모리 누수 방지

테스트 시나리오:
1. 1000개 메시지 부드러운 스크롤
2. 이미지 많은 채팅방 성능
3. 장시간 사용 시 메모리 안정성
4. 페이지 전환 시 정리 확인
```

---

## **🧪 단계별 테스트 체크포인트**

### **각 Phase 완료 시 필수 검증 항목:**

**Phase 1 완료 체크:**
- [ ] 두 브라우저에서 실시간 메시지 송수신 (p95 < 1초)
- [ ] 네트워크 끊김 후 자동 재연결 및 메시지 복구
- [ ] 방 입장/퇴장 플로우 100% 성공률

**Phase 2 완료 체크:**
- [ ] 타이핑 표시 1초 내 반영
- [ ] 온라인 사용자 수 실시간 정확도
- [ ] 메시지 상태 (전송중→완료→실패) 표시

**Phase 3 완료 체크:**
- [ ] 이미지/영상 S3 업로드 성공률 95%+
- [ ] CDN을 통한 미디어 로딩 최적화
- [ ] 10초 초과 영상/20MB 초과 파일 업로드 차단

**Phase 4 완료 체크:**
- [ ] 초당 100개 메시지 처리 성능
- [ ] 서버 장애 복구 시 메시지 무손실
- [ ] 메시지 순서 보장 100%

**Phase 5 완료 체크:**
- [ ] 100명 동시 접속 안정성
- [ ] 1000개+ 메시지 가상 스크롤 성능
- [ ] 24시간 연속 운영 메모리 안정성

---

## **💡 구현 시 주의사항**

### **1. 각 Phase를 완전히 완료한 후 다음 단계 진행**
- 이전 단계 테스트 100% 통과 확인
- 기능별 단위 테스트 작성 권장

### **2. 유저 경험 우선 순위 유지**
- 복잡한 기술보다 사용자가 체감하는 개선 우선
- 에러 상황에서의 사용자 안내 메시지 중요

### **3. 카카오톡 수준의 품질 기준**
- 메시지 지연 p95 < 1초
- 연결 끊김 후 3초 내 자동 복구
- 파일 업로드 성공률 95%+

---

## **🎯 구현 시작 포인트**

### **지금 당장 시작할 수 있는 Phase 1 작업:**

**1-1. 방 입장 플로우 완성**
- 파일: `backend/src/main/java/com/vibechat/service/room/RoomServiceImpl.java:82`
- 개선: joinRoom() 메서드의 닉네임 중복 체크 로직 강화

**1-2. WebSocket 연결 안정화**
- 파일: `frontend/src/services/ws/stompClient.ts:50`
- 개선: 연결 실패 시 사용자 친화적 에러 표시

**1-3. 메시지 상태 관리**
- 파일: `frontend/src/hooks/useMessages.ts`
- 신규: pending → sent → delivered 상태 추적

---

**이제 Phase 1-1 (방 입장 플로우)부터 시작하여 단계별로 구현하면서 각 단계마다 완전히 동작하는 채팅 서비스를 만들어갈 수 있습니다!**