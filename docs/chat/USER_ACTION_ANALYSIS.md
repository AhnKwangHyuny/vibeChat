# 🎯 채팅방 유저 액션 분석 및 구현 로드맵

## 📋 현재 상황 진단

### **문제점 발견:**
1. **API 불일치**: `useMessages` 훅이 Chat-Server의 제거된 REST API(`/api/rooms/{roomId}/messages`)를 호출
2. **서버 분리 미완성**: 메시지 조회가 API-Server로 라우팅되지 않음
3. **WebSocket 연결 오류**: Chat-Server 분리 후 WebSocket 엔드포인트 불일치

### **임시 해결책 제공:**
✅ **Mock API Controller** (`/api/mock/setup-test-room`) 생성
✅ **테스트 라우팅** (`/test-room`) 추가
✅ **즉시 테스트 가능한 환경** 구축

---

## 🔍 사용자 액션 분석

### **1. 채팅방 입장 플로우**
```
User Journey: 방 입장
├─ 1. URL 접근: /room/{roomId} 또는 /test-room
├─ 2. 방 정보 로딩: API-Server → getRoomById()
├─ 3. 인증 확인: Redux 사용자 상태 체크
├─ 4. 메시지 히스토리 로딩: API-Server → getMessages()
├─ 5. WebSocket 연결: Chat-Server → STOMP 구독
└─ 6. 실시간 이벤트 구독: /topic/rooms/{roomId}/*
```

### **2. 메시지 전송 액션**
```
Message Sending Actions:
├─ 2.1 텍스트 메시지
│   ├─ User Input → MessageInput 컴포넌트
│   ├─ WebSocket Send → Chat-Server /app/rooms/{roomId}/send
│   ├─ Redis Streams 저장 → 영구성 보장
│   └─ 실시간 브로드캐스트 → 다른 사용자들에게 즉시 전달
├─ 2.2 미디어 메시지 (사진/영상/GIF)
│   ├─ File Upload → API-Server /api/upload/media
│   ├─ S3 저장 + 썸네일 생성 → 미디어 처리
│   ├─ DB 메타데이터 저장 → MySQL
│   ├─ Redis Event 발행 → 서버간 통신
│   └─ Chat-Server 구독 → WebSocket 브로드캐스트
└─ 2.3 메시지 상태 관리
    ├─ Pending → 전송 중 (clientTempId 사용)
    ├─ Sent → Redis 저장 완료
    └─ Delivered → MySQL 영구 저장 완료
```

### **3. 실시간 상호작용 액션**
```
Real-time Interactions:
├─ 3.1 타이핑 표시 (Typing Indicators)
│   ├─ 입력 시작 → WebSocket /app/rooms/{roomId}/typing
│   ├─ Redis 임시 저장 (3초 TTL)
│   └─ 실시간 브로드캐스트 → 다른 사용자들에게 표시
├─ 3.2 온라인 상태 (Presence)
│   ├─ 방 입장 → PresenceService.userConnected()
│   ├─ Redis 세션 저장 → presence:room:{roomId}:sessions
│   └─ 온라인 사용자 수 브로드캐스트
├─ 3.3 메시지 반응 (Reactions) - 향후 구현
│   ├─ 이모지 클릭 → 반응 추가/제거
│   └─ 실시간 반응 카운트 업데이트
└─ 3.4 읽음 확인 (Read Receipts) - 향후 구현
    ├─ 메시지 읽음 이벤트 → MongoDB 업데이트
    └─ 발신자에게 읽음 상태 전송
```

### **4. UI/UX 액션**
```
User Interface Actions:
├─ 4.1 메시지 관련
│   ├─ 스크롤 → 무한 스크롤 + 이전 메시지 로딩
│   ├─ 메시지 액션 → 복사/답글/삭제 (ContextMenu)
│   ├─ 파일 드래그앤드롭 → 미디어 업로드
│   └─ 이모지 피커 → EmojiPicker 컴포넌트
├─ 4.2 방 관련
│   ├─ 사용자 목록 → UserList 사이드바
│   ├─ 방 설정 → 제목/설명 수정
│   └─ 방 나가기 → LeaveModal 확인
└─ 4.3 알림 관련
    ├─ 새 메시지 알림 → NotificationBadge
    ├─ 멘션 알림 → @username 감지
    └─ 브라우저 알림 → Web Notification API
```

---

## 🚨 긴급 수정 필요 사항

### **1. API 라우팅 수정**
```javascript
// 현재 문제: useMessages가 잘못된 엔드포인트 호출
// frontend/src/services/api/messages.ts
export const getMessages = (roomId, beforeId, limit) => {
  // 변경 전: /api/rooms/${roomId}/messages (Chat-Server - 제거됨)
  // 변경 후: /api/messages/${roomId} (API-Server)
};
```

### **2. WebSocket 엔드포인트 확인**
```javascript
// frontend/src/services/ws/stompClient.ts
// Chat-Server 포트 확인: ws://localhost:8081/ws
// Nginx 프록시 확인: /ws/* → chat-server:8081
```

### **3. 서버간 통신 구현**
```java
// API-Server: RedisEventPublisher 구현 필요
// Chat-Server: RedisEventSubscriber 구현 필요
```

---

## 📅 구현 우선순위 로드맵

### **🔥 긴급 (이번 주):**

#### **Day 1-2: API 라우팅 수정**
1. **메시지 조회 API 이동**
   - Chat-Server → API-Server로 이동
   - `MessageController.getMessages()` API-Server에 구현
   - 프론트엔드 API 엔드포인트 수정

2. **WebSocket 연결 복구**
   - Chat-Server WebSocket 설정 점검
   - Nginx 프록시 설정 확인
   - 프론트엔드 WebSocket 연결 테스트

#### **Day 3-4: 기본 채팅 기능 복구**
3. **텍스트 메시지 플로우 복구**
   - WebSocket 메시지 전송 테스트
   - Redis Streams 저장 확인
   - 실시간 브로드캐스트 테스트

4. **Presence & Typing 기능 복구**
   - 온라인 상태 표시 복구
   - 타이핑 인디케이터 복구

#### **Day 5: 통합 테스트**
5. **전체 플로우 테스트**
   - Mock 데이터로 E2E 테스트
   - 여러 브라우저로 동시 접속 테스트
   - 오류 상황 처리 테스트

### **🎯 주요 기능 (다음 주):**

#### **Week 2: Redis Event 시스템**
1. **RedisEventPublisher (API-Server)**
   - 미디어 업로드 후 이벤트 발행
   - 방 멤버십 변경 이벤트

2. **RedisEventSubscriber (Chat-Server)**
   - 미디어 메시지 이벤트 구독
   - WebSocket 브로드캐스트 연동

#### **Week 3: 미디어 메시지 구현**
1. **파일 업로드 플로우**
   - API-Server 업로드 API 강화
   - S3 저장 + 썸네일 생성
   - Event-driven 브로드캐스트

2. **미디어 메시지 표시**
   - 이미지/GIF 인라인 표시
   - 비디오 플레이어 구현
   - 다운로드/공유 기능

### **🚀 고급 기능 (3-4주차):**

#### **Week 4: UX 개선**
1. **메시지 반응 시스템**
   - 이모지 반응 추가/제거
   - 반응 카운트 실시간 업데이트

2. **읽음 확인 시스템**
   - 메시지 읽음 상태 추적
   - 읽은 사용자 목록 표시

#### **Week 5: 성능 최적화**
1. **가상 스크롤링**
   - 대량 메시지 처리 최적화
   - 메모리 사용량 개선

2. **오프라인 지원**
   - 오프라인 메시지 큐잉
   - 재연결 시 동기화

---

## 🏃‍♂️ 바로 시작할 액션 아이템

### **1. 즉시 테스트 환경 구축**
```bash
# 1. Mock 데이터 생성
curl -X POST http://localhost:8080/api/mock/setup-test-room

# 2. 테스트 채팅방 접속
# 브라우저: http://localhost:5173/test-room

# 3. WebSocket 연결 상태 확인
# 개발자 도구 → Network → WS 탭
```

### **2. 메시지 API 수정**
```javascript
// frontend/src/services/api/messages.ts 수정
export const getMessages = async (roomId: number, beforeId?: number, limit = 30) => {
  const response = await axiosInstance.get(`/api/messages/${roomId}`, {
    params: { beforeId, limit }
  });
  return response.data;
};
```

### **3. MessageController API-Server 이동**
```java
// api-server/src/main/java/com/vibechat/controller/MessageController.java
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @GetMapping("/{roomId}")
    public ResponseEntity<List<WebSocketMessageResponse>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "30") int limit) {
        // 구현
    }
}
```

---

## 🎯 성공 기준

### **단기 목표 (1주)**
- ✅ 테스트 채팅방에서 텍스트 메시지 송수신
- ✅ 실시간 타이핑 인디케이터 동작
- ✅ 온라인 사용자 수 표시
- ✅ 메시지 히스토리 로딩

### **중기 목표 (2-3주)**
- ✅ 파일 업로드 및 미디어 메시지 전송
- ✅ Redis Event 기반 서버간 통신
- ✅ 100개 이상 메시지 처리 성능

### **장기 목표 (4-5주)**
- ✅ 메시지 반응 및 읽음 확인
- ✅ 1000명 동시 접속 지원
- ✅ 완전한 오프라인 지원

**지금 바로 Mock API를 호출해서 테스트 채팅방을 만들고, /test-room으로 접속해서 현재 상태를 확인해보세요!** 🚀