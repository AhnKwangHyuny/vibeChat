# WebSocket + Redis Streams 통합 아키텍처 분석

## 현재 아키텍처 문제점

### 1. 분리된 처리 단계
```
WebSocket 연결 → WebSocketPresenceService (세션 관리)
방 입장 요청 → ChatRoomService (User Stream 생성)
```

**문제**: 두 단계가 독립적으로 동작하여 일관성 보장 어려움

### 2. User Stream 생성 타이밍
- 현재: 방 입장 시점에 User Stream 생성
- 문제: 사용자가 여러 방에 입장할 때마다 중복 호출

### 3. 세션 vs 사용자 혼재
- Presence: 세션 기반 (presence:session:{sessionId})
- Stream: 사용자 기반 (stream:user:{userId})
- 문제: 관리 주체가 달라 동기화 이슈

## 개선 방안

### Option 1: WebSocket 연결 시 통합 처리
```
WebSocket Handshake
→ WebSocketPresenceService.addUser()
→ DynamicStreamService.ensureUserStream() [통합]
→ 세션 + 사용자 스트림 동시 생성
```

**장점**: 연결 시점에 모든 인프라 준비 완료
**단점**: 방에 입장하지 않는 사용자도 Stream 생성

### Option 2: 방 입장 시 통합 처리 (현재 + 개선)
```
@MessageMapping("/rooms/{roomId}/join")
→ 사용자 Stream 존재 여부 확인
→ 없으면 생성, 있으면 재사용
→ 방별 Consumer Group 등록
```

**장점**: 필요시에만 리소스 생성
**단점**: 방 입장 시마다 존재 여부 확인 필요

### Option 3: 하이브리드 접근 (권장)
```
WebSocket 연결 시:
- 세션 정보 저장
- User Stream 생성 (한 번만)

방 입장 시:
- Room Stream 생성
- Consumer Group 등록
- 참가자 등록
```

## 구현 상세

### 1. WebSocketPresenceService 확장
```java
public void addUser(String sessionId, WebSocketSessionInfo sessionInfo) {
    // 기존: 세션 정보만 저장
    saveSessionInfo(sessionId, sessionInfo);

    // 추가: User Stream 생성 (중복 생성 방지)
    dynamicStreamService.ensureUserStreamOnce(sessionInfo.getUserId());
}
```

### 2. DynamicStreamService 개선
```java
// 사용자별 Stream 생성 (중복 방지)
public void ensureUserStreamOnce(Long userId) {
    String streamKey = "stream:user:" + userId;
    if (!streamExists(streamKey)) {
        createRedisStream(streamKey);
        consumerRegistrationService.addUserStreamToConsumerGroups(streamKey);
    }
}

// 방별 Stream은 기존 유지
public void ensureRoomStream(Long roomId) {
    // 기존 로직 유지
}
```

### 3. Redis Key 관리 전략
```
# 세션 관리
presence:session:{sessionId} → {userId, nickname, avatarUrl, timestamp}

# 사용자 스트림 (글로벌, 1:1 매핑)
stream:user:{userId} → 사용자별 개인 메시지/알림

# 방 스트림 (방별)
stream:room:{roomId} → 방별 채팅 메시지

# 방 참가자
room:{roomId}:participants → Set<userId>

# 스트림 존재 여부 캐시
streams:created → Set<streamKey> (중복 생성 방지)
```

## 동시 입장 처리

### 케이스 1: 동일 사용자, 다른 세션
```
User A: Session-1에서 Room-1 입장
User A: Session-2에서 Room-2 입장

처리:
- User Stream은 이미 존재하므로 재사용
- 각각 다른 방 참가자로 등록
```

### 케이스 2: 동일 사용자, 동일 방 재입장
```
User A: Session-1에서 Room-1 입장
User A: Session-1에서 Room-1 재입장

처리:
- 이미 참가자로 등록되어 있으면 중복 방지
- 또는 최신 세션으로 업데이트
```

## 성능 최적화

### 1. Stream 존재 여부 캐시
```java
@Component
public class StreamExistenceCache {
    private final Set<String> createdStreams = ConcurrentHashMap.newKeySet();

    public boolean streamExists(String streamKey) {
        return createdStreams.contains(streamKey) || redisStreamExists(streamKey);
    }
}
```

### 2. 배치 Consumer 등록
```java
// 여러 스트림을 한번에 Consumer Group에 등록
public void registerMultipleStreams(List<String> streamKeys, String consumerGroup) {
    for (String streamKey : streamKeys) {
        registerStreamForConsumer(streamKey, consumerGroup, consumer);
    }
}
```

## 추천 구현 순서

1. **DynamicStreamService 개선**: 중복 방지 로직 추가
2. **WebSocketPresenceService 확장**: User Stream 생성 통합
3. **StreamExistenceCache 구현**: 성능 최적화
4. **통합 테스트**: 동시 접속 시나리오 검증

이 구조로 개선하면 **일관성**, **성능**, **확장성**을 모두 확보할 수 있습니다.