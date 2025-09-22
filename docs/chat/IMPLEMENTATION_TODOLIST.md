# 🚀 VibeChat 메시지 시스템 구현 TodoList
**Based on:** PRD v2.0 + TRD v2.0
**Timeline:** 10주 (70일)
**Start Date:** 2025-09-22

---

## 📋 전체 구현 로드맵

### **Phase 1: 기반 인프라 구축 (2주)**
- Redis Streams 메시지 큐 시스템 구현
- 마이크로서비스 분리 완료
- 기본 실시간 메시징 복구

### **Phase 2: 핵심 메시징 기능 (3주)**
- 텍스트 메시지 송수신 안정화
- 메시지 상태 관리 (pending→sent→delivered)
- 실시간 상호작용 (typing, presence)

### **Phase 3: 미디어 처리 (2주)**
- S3 파일 업로드 시스템
- 미디어 메시지 처리 파이프라인
- CDN 최적화

### **Phase 4: 성능 최적화 (2주)**
- 프론트엔드 최적화 (Virtual Scrolling)
- 백엔드 성능 튜닝
- 모니터링 시스템

### **Phase 5: 테스트 & 배포 (1주)**
- 통합 테스트
- 성능 테스트
- 프로덕션 배포

---

## 🎯 Phase 1: 기반 인프라 구축 (14일)

### **Week 1: Redis Streams 메시지 큐 구현**

#### **Day 1-2: Redis Streams 기본 구조 설정**

**[TASK-001] Redis Streams Configuration 구현**
- **Priority:** 🔴 HIGH
- **Estimate:** 4시간
- **Files:**
  - `chat-server/src/main/java/com/vibechat/config/RedisStreamsConfig.java` (신규)
  - `chat-server/src/main/resources/application.yml`
- **Requirements:**
  - StreamMessageListenerContainer Bean 설정
  - Redis Template 설정 (Jackson serializer)
  - Consumer Group 관리 로직
- **Acceptance Criteria:**
  - [ ] Redis Streams connection 성공
  - [ ] Consumer group 자동 생성
  - [ ] Connection pool 설정 완료

**[TASK-002] Message Stream Producer 구현**
- **Priority:** 🔴 HIGH
- **Estimate:** 6시간
- **Files:**
  - `chat-server/src/main/java/com/vibechat/service/MessageStreamProducer.java` (신규)
- **Requirements:**
  - XADD 명령으로 메시지 스트림에 추가
  - 메시지 구조 표준화 (JSON)
  - 에러 처리 및 로깅
- **Acceptance Criteria:**
  - [ ] 텍스트 메시지 Redis Stream에 저장 성공
  - [ ] clientTempId 기반 중복 방지
  - [ ] Stream 최대 길이 제한 (10,000개)

#### **Day 3-4: Message Consumer 및 MySQL 저장**

**[TASK-003] Message Stream Consumer 구현**
- **Priority:** 🔴 HIGH
- **Estimate:** 8시간
- **Files:**
  - `api-server/src/main/java/com/vibechat/service/MessageStreamConsumer.java` (신규)
  - `api-server/src/main/java/com/vibechat/repository/MessageRepository.java`
- **Requirements:**
  - XREADGROUP으로 메시지 consume
  - 배치 처리 (50개 단위)
  - MySQL 배치 저장 (@BatchSize)
  - XACK로 acknowledgment
- **Acceptance Criteria:**
  - [ ] Redis Stream → MySQL 자동 저장
  - [ ] 배치 처리 성능 확인 (초당 100개 처리)
  - [ ] 장애 시 재처리 로직 동작

**[TASK-004] Dead Letter Queue 처리**
- **Priority:** 🟡 MEDIUM
- **Estimate:** 4시간
- **Files:**
  - `api-server/src/main/java/com/vibechat/service/DeadLetterQueueService.java` (신규)
- **Requirements:**
  - 실패한 메시지 DLQ로 이동
  - 재처리 스케줄러 구현
  - 관리자 알림 시스템
- **Acceptance Criteria:**
  - [ ] 3회 실패 시 DLQ 이동
  - [ ] DLQ 메시지 수동 복구 가능
  - [ ] 지연 시간 알람 설정

#### **Day 5-7: WebSocket Integration**

**[TASK-005] Chat-Server WebSocket 메시지 핸들러 수정**
- **Priority:** 🔴 HIGH
- **Estimate:** 6시간
- **Files:**
  - `chat-server/src/main/java/com/vibechat/controller/ChatMessageController.java`
- **Requirements:**
  - 기존 직접 DB 저장을 Redis Streams로 변경
  - 즉시 WebSocket 브로드캐스트 유지
  - Rate limiting 적용
- **Acceptance Criteria:**
  - [ ] 텍스트 메시지 실시간 송수신 (≤1초)
  - [ ] Rate limiting 동작 (분당 20개)
  - [ ] 메시지 순서 보장

**[TASK-006] 프론트엔드 useMessages 훅 복원**
- **Priority:** 🔴 HIGH
- **Estimate:** 4시간
- **Files:**
  - `frontend/src/hooks/useMessages.ts`
  - `frontend/src/pages/Room.tsx`
- **Requirements:**
  - 주석 처리된 WebSocket 연결 복원
  - API calls 복원 (API-Server 라우팅)
  - 에러 처리 강화
- **Acceptance Criteria:**
  - [ ] 채팅방 입장 시 메시지 히스토리 로딩
  - [ ] 실시간 메시지 수신
  - [ ] 네트워크 에러 시 재연결

---

### **Week 2: 마이크로서비스 통신 구현**

#### **Day 8-10: Redis Event Pub/Sub 시스템**

**[TASK-007] API-Server Event Publisher 구현**
- **Priority:** 🔴 HIGH
- **Estimate:** 6시간
- **Files:**
  - `api-server/src/main/java/com/vibechat/service/MediaEventPublisher.java` (신규)
  - `api-server/src/main/java/com/vibechat/dto/MediaMessageEvent.java` (신규)
- **Requirements:**
  - 미디어 업로드 완료 시 이벤트 발행
  - Redis Pub/Sub 채널 활용
  - 이벤트 데이터 구조 표준화
- **Acceptance Criteria:**
  - [ ] 파일 업로드 후 이벤트 발행
  - [ ] JSON 직렬화/역직렬화 성공
  - [ ] 이벤트 발행 실패 시 재시도

**[TASK-008] Chat-Server Event Subscriber 구현**
- **Priority:** 🔴 HIGH
- **Estimate:** 6시간
- **Files:**
  - `chat-server/src/main/java/com/vibechat/service/MediaEventSubscriber.java` (신규)
- **Requirements:**
  - 미디어 이벤트 수신 및 처리
  - WebSocket 브로드캐스트 연동
  - Redis Streams에도 저장
- **Acceptance Criteria:**
  - [ ] 미디어 이벤트 수신 성공
  - [ ] 실시간 미디어 메시지 브로드캐스트
  - [ ] 메시지 순서 유지

#### **Day 11-14: 통합 테스트 및 디버깅**

**[TASK-009] E2E 메시지 플로우 테스트**
- **Priority:** 🔴 HIGH
- **Estimate:** 8시간
- **Files:**
  - `backend/src/test/java/integration/MessageFlowIntegrationTest.java` (신규)
- **Requirements:**
  - 텍스트 메시지 전체 플로우 테스트
  - 두 브라우저 동시 테스트
  - 성능 측정 (지연 시간)
- **Acceptance Criteria:**
  - [ ] 메시지 전송→수신 1초 내 완료
  - [ ] 메시지 순서 정확성 100%
  - [ ] 메시지 누락률 0%

**[TASK-010] 로깅 및 모니터링 기본 설정**
- **Priority:** 🟡 MEDIUM
- **Estimate:** 4시간
- **Files:**
  - `backend/src/main/resources/logback-spring.xml`
  - Application properties files
- **Requirements:**
  - 구조화된 로깅 (JSON format)
  - 메시지 처리 메트릭 수집
  - 에러 로그 분류
- **Acceptance Criteria:**
  - [ ] 모든 메시지 처리 로깅
  - [ ] 성능 메트릭 출력
  - [ ] 에러 발생 시 상세 로그

---

## 🎯 Phase 2: 핵심 메시징 기능 (21일)

### **Week 3: 메시지 상태 관리**

#### **Day 15-17: 메시지 상태 시스템 구현**

**[TASK-011] 메시지 상태 DTO 구현**
- **Priority:** 🔴 HIGH
- **Estimate:** 3시간
- **Files:**
  - `frontend/src/types/message.ts`
  - `backend/src/main/java/com/vibechat/dto/MessageStatusDto.java` (신규)
- **Requirements:**
  - PENDING, SENT, DELIVERED, FAILED 상태 정의
  - 상태 전환 로직
  - 타임스탬프 추가
- **Acceptance Criteria:**
  - [ ] 메시지 상태 타입 정의 완료
  - [ ] 상태 전환 규칙 명확화
  - [ ] 프론트엔드-백엔드 타입 일치

**[TASK-012] 프론트엔드 메시지 상태 UI**
- **Priority:** 🔴 HIGH
- **Estimate:** 6시간
- **Files:**
  - `frontend/src/components/chat/MessageBubble.tsx`
  - `frontend/src/components/chat/MessageStatus.tsx` (신규)
- **Requirements:**
  - 상태별 아이콘 표시 (로딩, 체크마크, 에러)
  - 애니메이션 효과
  - 재전송 버튼 (실패 시)
- **Acceptance Criteria:**
  - [ ] 전송 중 로딩 스피너 표시
  - [ ] 성공 시 체크마크 표시
  - [ ] 실패 시 재시도 버튼 표시

#### **Day 18-21: 메시지 상태 업데이트 시스템**

**[TASK-013] 백엔드 메시지 상태 브로드캐스트**
- **Priority:** 🔴 HIGH
- **Estimate:** 8시간
- **Files:**
  - `chat-server/src/main/java/com/vibechat/service/MessageStatusService.java` (신규)
- **Requirements:**
  - DB 저장 완료 시 DELIVERED 상태 브로드캐스트
  - clientTempId 기반 메시지 매칭
  - WebSocket을 통한 상태 업데이트
- **Acceptance Criteria:**
  - [ ] DB 저장 완료 시 자동 상태 업데이트
  - [ ] 정확한 메시지 매칭 (clientTempId)
  - [ ] 실시간 상태 반영 (≤500ms)

**[TASK-014] 프론트엔드 메시지 상태 동기화**
- **Priority:** 🔴 HIGH
- **Estimate:** 6시간
- **Files:**
  - `frontend/src/hooks/useMessages.ts`
  - `frontend/src/store/slices/messagesSlice.ts`
- **Requirements:**
  - 상태 업데이트 WebSocket 메시지 처리
  - Redux 상태 동기화
  - Optimistic UI 업데이트
- **Acceptance Criteria:**
  - [ ] 실시간 상태 업데이트 수신
  - [ ] Redux 상태 정확한 동기화
  - [ ] 낙관적 UI 업데이트 동작

---

### **Week 4: 실시간 상호작용**

#### **Day 22-24: 타이핑 인디케이터 고도화**

**[TASK-015] 타이핑 상태 Redis TTL 관리**
- **Priority:** 🟡 MEDIUM
- **Estimate:** 4시간
- **Files:**
  - `chat-server/src/main/java/com/vibechat/service/PresenceService.java`
- **Requirements:**
  - Redis SET with TTL 3초
  - 자동 타이핑 종료 처리
  - 동시 타이핑자 목록 관리
- **Acceptance Criteria:**
  - [ ] 3초 후 자동 타이핑 상태 제거
  - [ ] 여러 명 동시 타이핑 지원
  - [ ] 메모리 누수 방지

**[TASK-016] 프론트엔드 타이핑 UI 개선**
- **Priority:** 🟡 MEDIUM
- **Estimate:** 4시간
- **Files:**
  - `frontend/src/components/chat/TypingIndicator.tsx`
- **Requirements:**
  - 여러 명 타이핑 시 표시 개선
  - 애니메이션 효과 추가
  - 자신은 제외하고 표시
- **Acceptance Criteria:**
  - [ ] "김철수, 이영희가 입력 중..." 형태 표시
  - [ ] 점 애니메이션 효과
  - [ ] 본인 타이핑 상태 제외

#### **Day 25-28: Presence 시스템 안정화**

**[TASK-017] WebSocket 연결 상태 관리**
- **Priority:** 🔴 HIGH
- **Estimate:** 8시간
- **Files:**
  - `chat-server/src/main/java/com/vibechat/config/WebSocketEventListener.java`
- **Requirements:**
  - 연결/해제 이벤트 처리 개선
  - 중복 세션 방지
  - Heartbeat 실패 시 정리
- **Acceptance Criteria:**
  - [ ] 브라우저 종료 시 즉시 오프라인 처리
  - [ ] 네트워크 끊김 후 재연결 시 중복 방지
  - [ ] 온라인 카운트 정확도 99%+

**[TASK-018] 프론트엔드 연결 상태 표시**
- **Priority:** 🟡 MEDIUM
- **Estimate:** 4시간
- **Files:**
  - `frontend/src/components/chat/ConnectionStatus.tsx` (신규)
  - `frontend/src/hooks/useWebSocketStatus.ts` (신규)
- **Requirements:**
  - 연결 상태 UI 표시 (온라인/오프라인/재연결중)
  - 재연결 진행률 표시
  - 오프라인 시 사용자 안내
- **Acceptance Criteria:**
  - [ ] 연결 상태 실시간 표시
  - [ ] 재연결 시도 횟수 표시
  - [ ] 명확한 상태별 메시지

---

### **Week 5: 에러 처리 및 안정성**

#### **Day 29-35: 종합적인 에러 처리**

**[TASK-019] 백엔드 예외 처리 체계화**
- **Priority:** 🔴 HIGH
- **Estimate:** 6시간
- **Files:**
  - `backend/src/main/java/com/vibechat/exception/MessageProcessingException.java` (신규)
  - `backend/src/main/java/com/vibechat/controller/GlobalExceptionHandler.java`
- **Requirements:**
  - 메시지 처리 관련 커스텀 예외
  - 상세한 에러 코드 체계
  - 사용자 친화적 에러 메시지
- **Acceptance Criteria:**
  - [ ] 모든 메시지 처리 예외 커버
  - [ ] 에러 코드별 분류
  - [ ] 로깅 및 모니터링 연동

**[TASK-020] 프론트엔드 에러 처리 및 재시도**
- **Priority:** 🔴 HIGH
- **Estimate:** 8시간
- **Files:**
  - `frontend/src/components/chat/ErrorBoundary.tsx` (신규)
  - `frontend/src/utils/retryLogic.ts` (신규)
  - `frontend/src/hooks/useMessages.ts`
- **Requirements:**
  - 메시지 전송 실패 시 재시도 로직
  - 네트워크 에러 시 자동 재연결
  - 사용자 친화적 에러 메시지
- **Acceptance Criteria:**
  - [ ] 전송 실패 시 3회 자동 재시도
  - [ ] 네트워크 끊김 시 지수 백오프 재연결
  - [ ] 명확한 에러 메시지 표시

---

## 🎯 Phase 3: 미디어 처리 (14일)

### **Week 6: S3 업로드 시스템**

#### **Day 36-38: S3 Configuration & Upload Service**

**[TASK-021] AWS S3 설정 및 의존성 추가**
- **Priority:** 🔴 HIGH
- **Estimate:** 3시간
- **Files:**
  - `api-server/pom.xml`
  - `api-server/src/main/resources/application.yml`
  - `api-server/src/main/java/com/vibechat/config/S3Config.java` (신규)
- **Requirements:**
  - AWS SDK 의존성 추가
  - S3 클라이언트 Bean 설정
  - CloudFront 설정
- **Acceptance Criteria:**
  - [ ] S3 연결 테스트 성공
  - [ ] CloudFront 도메인 설정 완료
  - [ ] 환경별 버킷 분리 (dev/prod)

**[TASK-022] S3FileUploadService 구현**
- **Priority:** 🔴 HIGH
- **Estimate:** 10시간
- **Files:**
  - `api-server/src/main/java/com/vibechat/service/S3FileUploadService.java` (신규)
  - `api-server/src/main/java/com/vibechat/service/ThumbnailService.java` (신규)
- **Requirements:**
  - 멀티파트 파일 업로드
  - 이미지 썸네일 자동 생성
  - 비디오 포스터 프레임 추출
  - 파일 형식/크기 검증
- **Acceptance Criteria:**
  - [ ] 20MB 파일 업로드 성공
  - [ ] 썸네일 자동 생성 (512px)
  - [ ] 10초 초과 비디오 업로드 차단

#### **Day 39-42: 미디어 업로드 API**

**[TASK-023] 미디어 업로드 컨트롤러**
- **Priority:** 🔴 HIGH
- **Estimate:** 6시간
- **Files:**
  - `api-server/src/main/java/com/vibechat/controller/MediaUploadController.java`
  - `api-server/src/main/java/com/vibechat/dto/MediaUploadResponse.java` (신규)
- **Requirements:**
  - 파일 업로드 진행률 API
  - 업로드 상태 조회 API
  - 에러 상황 핸들링
- **Acceptance Criteria:**
  - [ ] 업로드 진행률 실시간 조회
  - [ ] 업로드 완료 시 메타데이터 반환
  - [ ] 실패 시 상세한 에러 정보

**[TASK-024] 프론트엔드 파일 업로드 UI**
- **Priority:** 🔴 HIGH
- **Estimate:** 10시간
- **Files:**
  - `frontend/src/components/chat/FileUpload.tsx` (신규)
  - `frontend/src/components/chat/FilePreview.tsx` (신규)
  - `frontend/src/hooks/useFileUpload.ts` (신규)
- **Requirements:**
  - 드래그 앤 드롭 인터페이스
  - 업로드 진행률 표시
  - 미리보기 기능
  - 업로드 취소 기능
- **Acceptance Criteria:**
  - [ ] 드래그 앤 드롭 업로드
  - [ ] 실시간 진행률 표시 (0-100%)
  - [ ] 이미지/비디오 미리보기
  - [ ] 업로드 중 취소 가능

---

### **Week 7: 미디어 메시지 파이프라인**

#### **Day 43-49: 미디어 메시지 처리 완성**

**[TASK-025] 미디어 이벤트 통합**
- **Priority:** 🔴 HIGH
- **Estimate:** 8시간
- **Files:**
  - 이미 구현된 MediaEventPublisher, MediaEventSubscriber 연동
  - `api-server/src/main/java/com/vibechat/service/MediaMessageService.java` (신규)
- **Requirements:**
  - 업로드 완료 → 이벤트 발행 → 실시간 브로드캐스트 파이프라인
  - DB 저장 완료 후 이벤트 발행
  - 미디어 메시지 Redis Streams 저장
- **Acceptance Criteria:**
  - [ ] 파일 업로드 → 실시간 메시지 전달 (≤3초)
  - [ ] 이벤트 발행 실패 시 재시도
  - [ ] 메시지 순서 보장

**[TASK-026] 프론트엔드 미디어 메시지 표시**
- **Priority:** 🔴 HIGH
- **Estimate:** 12시간
- **Files:**
  - `frontend/src/components/chat/MediaMessage.tsx` (신규)
  - `frontend/src/components/chat/ImageViewer.tsx` (신규)
  - `frontend/src/components/chat/VideoPlayer.tsx` (신규)
- **Requirements:**
  - 썸네일 우선 로딩
  - 클릭 시 전체화면 모달
  - 비디오 포스터 및 재생 버튼
  - Lazy loading 최적화
- **Acceptance Criteria:**
  - [ ] 썸네일 → 원본 순차 로딩
  - [ ] 이미지 클릭 시 전체화면 뷰어
  - [ ] 비디오 재생 컨트롤
  - [ ] 로딩 중 스켈레톤 UI

---

## 🎯 Phase 4: 성능 최적화 (14일)

### **Week 8: 프론트엔드 최적화**

#### **Day 50-53: Virtual Scrolling & Memory Optimization**

**[TASK-027] React Virtual Scrolling 구현**
- **Priority:** 🟡 MEDIUM
- **Estimate:** 12시간
- **Files:**
  - `frontend/src/components/chat/VirtualizedMessageList.tsx` (신규)
  - `frontend/package.json` (react-window 추가)
- **Requirements:**
  - react-window 기반 가상 스크롤링
  - 1000개+ 메시지 부드러운 스크롤
  - 동적 아이템 높이 지원
- **Acceptance Criteria:**
  - [ ] 1000개 메시지 부드러운 스크롤
  - [ ] 메모리 사용량 200MB 이하 유지
  - [ ] 스크롤 성능 60fps

**[TASK-028] 이미지 Lazy Loading 최적화**
- **Priority:** 🟡 MEDIUM
- **Estimate:** 6시간
- **Files:**
  - `frontend/src/components/chat/LazyImage.tsx` (TRD 참조)
  - `frontend/src/hooks/useIntersectionObserver.ts` (신규)
- **Requirements:**
  - Intersection Observer API 활용
  - Progressive loading (썸네일→원본)
  - 로딩 실패 시 fallback
- **Acceptance Criteria:**
  - [ ] 화면에 보이는 이미지만 로드
  - [ ] 점진적 이미지 로딩
  - [ ] 네트워크 실패 시 재시도

#### **Day 54-56: Redux & Component Optimization**

**[TASK-029] Redux 상태 정규화**
- **Priority:** 🟡 MEDIUM
- **Estimate:** 8시간
- **Files:**
  - `frontend/src/store/slices/messagesSlice.ts`
  - `frontend/src/store/slices/usersSlice.ts`
- **Requirements:**
  - 정규화된 상태 구조 (entities pattern)
  - 메모이제이션 최적화
  - 불필요한 리렌더링 방지
- **Acceptance Criteria:**
  - [ ] 정규화된 엔티티 구조
  - [ ] useSelector 메모이제이션
  - [ ] 리렌더링 횟수 50% 감소

---

### **Week 9: 백엔드 성능 튜닝**

#### **Day 57-63: Database & Redis Optimization**

**[TASK-030] Database 쿼리 최적화**
- **Priority:** 🔴 HIGH
- **Estimate:** 8시간
- **Files:**
  - `api-server/src/main/java/com/vibechat/repository/MessageRepository.java`
  - Database migration files
- **Requirements:**
  - 복합 인덱스 추가 (room_id, created_at DESC)
  - N+1 문제 해결 (fetch join)
  - 배치 쿼리 최적화
- **Acceptance Criteria:**
  - [ ] 메시지 조회 쿼리 100ms 이하
  - [ ] 배치 insert 성능 향상
  - [ ] 인덱스 적중률 95%+

**[TASK-031] Redis Connection Pool 최적화**
- **Priority:** 🔴 HIGH
- **Estimate:** 4시간
- **Files:**
  - `backend/src/main/resources/application.yml`
  - `backend/src/main/java/com/vibechat/config/RedisConfig.java`
- **Requirements:**
  - Lettuce 연결 풀 설정 튜닝
  - Timeout 및 재시도 설정
  - 모니터링 메트릭 추가
- **Acceptance Criteria:**
  - [ ] Redis 연결 지연 10ms 이하
  - [ ] 연결 풀 효율적 활용
  - [ ] 연결 실패 시 자동 재시도

**[TASK-032] JVM 및 Spring Boot 튜닝**
- **Priority:** 🟡 MEDIUM
- **Estimate:** 6시간
- **Files:**
  - `backend/Dockerfile`
  - `backend/src/main/resources/application.yml`
- **Requirements:**
  - JVM 힙 메모리 설정 최적화
  - Spring Boot actuator 성능 메트릭
  - 가비지 컬렉션 튜닝
- **Acceptance Criteria:**
  - [ ] 메모리 사용량 안정화
  - [ ] GC 일시정지 시간 50ms 이하
  - [ ] 처리량 20% 향상

---

## 🎯 Phase 5: 테스트 & 배포 (7일)

### **Week 10: 통합 테스트 & 프로덕션 배포**

#### **Day 64-67: 성능 테스트**

**[TASK-033] 부하 테스트 구현**
- **Priority:** 🔴 HIGH
- **Estimate:** 12시간
- **Files:**
  - `backend/src/test/java/performance/LoadTest.java` (신규)
  - JMeter 테스트 스크립트
- **Requirements:**
  - 1000명 동시 접속 시뮬레이션
  - 초당 500개 메시지 전송
  - 성능 지표 수집
- **Acceptance Criteria:**
  - [ ] 1000 동시 접속 안정성
  - [ ] 메시지 지연 p95 ≤ 1초
  - [ ] 에러율 ≤ 0.1%

**[TASK-034] E2E 테스트 자동화**
- **Priority:** 🟡 MEDIUM
- **Estimate:** 8시간
- **Files:**
  - `frontend/cypress/integration/chat.spec.ts` (신규)
- **Requirements:**
  - 주요 사용자 플로우 자동 테스트
  - 브라우저 간 호환성 테스트
  - 성능 측정 포함
- **Acceptance Criteria:**
  - [ ] 전체 채팅 플로우 자동 테스트
  - [ ] 크로스 브라우저 테스트
  - [ ] 성능 회귀 감지

#### **Day 68-70: 프로덕션 배포**

**[TASK-035] Docker 프로덕션 설정**
- **Priority:** 🔴 HIGH
- **Estimate:** 6시간
- **Files:**
  - `docker-compose.prod.yml`
  - `nginx/nginx.prod.conf`
- **Requirements:**
  - Multi-stage Docker 빌드
  - 환경별 설정 분리
  - Health check 설정
- **Acceptance Criteria:**
  - [ ] 프로덕션 환경 자동 배포
  - [ ] 무중단 배포 지원
  - [ ] 모니터링 대시보드 연동

**[TASK-036] 모니터링 & 알림 설정**
- **Priority:** 🔴 HIGH
- **Estimate:** 6시간
- **Files:**
  - Prometheus/Grafana 설정
  - AlertManager 규칙
- **Requirements:**
  - 성능 메트릭 대시보드
  - 장애 상황 자동 알림
  - 로그 집계 시스템
- **Acceptance Criteria:**
  - [ ] 실시간 성능 대시보드
  - [ ] 장애 시 즉시 알림
  - [ ] 로그 검색 및 분석 가능

---

## 🎯 우선순위별 Critical Path

### **🔴 HIGH Priority (반드시 완료)**
1. **TASK-001 ~ 006**: Redis Streams 기본 구조 (Week 1)
2. **TASK-007 ~ 009**: 마이크로서비스 통신 (Week 2)
3. **TASK-011 ~ 014**: 메시지 상태 관리 (Week 3)
4. **TASK-017**: WebSocket 연결 상태 관리 (Week 4)
5. **TASK-019 ~ 020**: 에러 처리 체계화 (Week 5)
6. **TASK-021 ~ 026**: S3 업로드 및 미디어 처리 (Week 6-7)
7. **TASK-030 ~ 031**: 성능 최적화 (Week 9)
8. **TASK-033 ~ 036**: 테스트 및 배포 (Week 10)

### **🟡 MEDIUM Priority (시간 여유 시 완료)**
- TASK-004, 015, 016, 018, 027, 028, 029, 032, 034

### **⚪ LOW Priority (향후 개선)**
- 메시지 검색, 읽음 확인, 답글 기능

---

## 🛠️ 개발 환경 설정

### **필수 도구 설치**
```bash
# Java 21
sdk install java 21.0.1-oracle

# Node.js 18+
nvm install 18
nvm use 18

# Redis (via Docker)
docker run -d --name redis -p 6379:6379 redis:7-alpine

# MySQL (via Docker)
docker run -d --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=password mysql:8.0

# AWS CLI (S3 업로드용)
pip install awscli
aws configure
```

### **IDE 설정**
- **IntelliJ IDEA**: Spring Boot, Lombok 플러그인
- **VS Code**: ES7 React/Redux/GraphQL/React-Native snippets

---

## 📊 진행 상황 추적

### **완료 기준**
- [ ] **Week 1 완료**: 기본 텍스트 메시지 실시간 송수신
- [ ] **Week 2 완료**: 마이크로서비스 통신 안정화
- [ ] **Week 3 완료**: 메시지 상태 표시 구현
- [ ] **Week 4 완료**: 실시간 상호작용 안정화
- [ ] **Week 5 완료**: 에러 처리 완료
- [ ] **Week 6-7 완료**: 미디어 업로드/표시 구현
- [ ] **Week 8 완료**: 프론트엔드 성능 최적화
- [ ] **Week 9 완료**: 백엔드 성능 튜닝
- [ ] **Week 10 완료**: 프로덕션 배포

### **일일 점검 항목**
- [ ] 해당일 TASK 완료 여부
- [ ] 단위 테스트 통과 여부
- [ ] 성능 지표 측정 (응답 시간)
- [ ] 메모리 누수 확인
- [ ] 에러 로그 검토

---

**이 TodoList는 실제 구현을 위한 완전한 가이드입니다. 각 TASK는 독립적으로 실행 가능하며, 우선순위에 따라 유연하게 조정할 수 있습니다.**