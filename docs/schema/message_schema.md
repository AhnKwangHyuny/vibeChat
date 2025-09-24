# Message Schema Design Document

## 문서 정보
- 작성일: 2024-09-23
- 작성자: DBA Team
- 버전: 1.0
- 검토자: Backend Team Lead

## 개요

실시간 채팅 서비스의 메시지 데이터를 위한 MongoDB 스키마 설계 문서입니다.
기존 MySQL의 messages 테이블을 MongoDB로 마이그레이션하면서 확장성과 성능을 고려한 설계를 진행합니다.

## 설계 원칙

### 데이터 특성 분석
- **생성 빈도**: 초당 수백 건 (피크 시간)
- **수정 빈도**: 거의 없음 (Immutable)
- **조회 패턴**: 시간순 역순, 방별 그룹핑
- **보존 기간**: 영구 보존 (법적 요구사항)
- **확장성**: 월 1억 건 이상 예상

### 기술적 요구사항
- 샤딩 지원 (수평 확장)
- 시계열 데이터 최적화
- 미디어 메타데이터 지원
- 메시지 상태 추적
- 전문 검색 지원 (향후)

## 컬렉션 설계

### messages 컬렉션

```javascript
{
  "_id": ObjectId("..."),
  "msgId": "msg_20240923_1234567890",     // 비즈니스 식별자
  "roomId": 1001,                         // 샤딩 키 (ChatRoom.id 참조)
  "userId": 5678,                         // User.id 참조
  "userInfo": {                           // 비정규화 (성능 최적화)
    "nickname": "user123",
    "avatarUrl": "https://..."
  },
  "type": "TEXT",                         // TEXT, IMAGE, GIF, VIDEO, SYSTEM, FILE
  "content": {
    "text": "메시지 내용",                    // 텍스트 메시지
    "media": {                            // 미디어 정보 (null 가능)
      "url": "https://s3.../file.mp4",
      "thumbnailUrl": "https://s3.../thumb.jpg",
      "mimeType": "video/mp4",
      "size": 2048576,
      "duration": 15.5,
      "width": 1920,
      "height": 1080,
      "metadata": {                       // 확장 가능한 메타데이터
        "transcoded": true,
        "quality": "HD"
      }
    },
    "system": {                           // 시스템 메시지 (null 가능)
      "action": "USER_JOINED",
      "data": {"targetUserId": 9999}
    }
  },
  "clientInfo": {
    "tempId": "temp_abc123",              // 클라이언트 임시 ID
    "platform": "WEB",                   // WEB, IOS, ANDROID
    "version": "1.2.3"
  },
  "status": {
    "isDeleted": false,
    "isEdited": false,
    "editedAt": null,
    "deletedAt": null,
    "reason": null                        // 삭제 사유
  },
  "delivery": {
    "deliveredTo": [],                    // 전달 완료된 유저 목록
    "readBy": [                           // 읽음 처리 정보
      {
        "userId": 1234,
        "readAt": ISODate("2024-09-23T10:30:00Z")
      }
    ],
    "lastDeliveryAttempt": ISODate("2024-09-23T10:25:00Z")
  },
  "metrics": {                            // 분석용 메트릭
    "reactions": {
      "👍": ["user1", "user2"],
      "❤️": ["user3"]
    },
    "mentions": ["@user123", "@channel"],  // 멘션 정보
    "hashtags": ["#general", "#dev"]      // 해시태그
  },
  "timestamp": ISODate("2024-09-23T10:25:00Z"),
  "createdAt": ISODate("2024-09-23T10:25:00Z"),
  "updatedAt": ISODate("2024-09-23T10:25:00Z")
}
```

## 인덱스 설계

### 기본 인덱스
```javascript
// 1. 샤딩 키 (복합 인덱스)
db.messages.createIndex(
  { "roomId": 1, "timestamp": -1 },
  { name: "idx_roomId_timestamp" }
)

// 2. 유저별 메시지 조회
db.messages.createIndex(
  { "userId": 1, "timestamp": -1 },
  { name: "idx_userId_timestamp" }
)

// 3. 클라이언트 임시 ID (ACK 처리용)
db.messages.createIndex(
  { "clientInfo.tempId": 1 },
  {
    name: "idx_clientTempId",
    sparse: true,
    expireAfterSeconds: 86400  // 24시간 후 자동 삭제
  }
)

// 4. 메시지 타입별 조회
db.messages.createIndex(
  { "roomId": 1, "type": 1, "timestamp": -1 },
  { name: "idx_roomId_type_timestamp" }
)

// 5. 전문 검색용 (향후)
db.messages.createIndex(
  { "content.text": "text" },
  {
    name: "idx_content_text",
    sparse: true,
    language_override: "language"
  }
)
```

### 복합 인덱스 (성능 최적화)
```javascript
// 읽음 상태 조회용
db.messages.createIndex(
  { "roomId": 1, "delivery.readBy.userId": 1 },
  { name: "idx_roomId_readBy" }
)

// 삭제되지 않은 메시지 조회
db.messages.createIndex(
  { "roomId": 1, "status.isDeleted": 1, "timestamp": -1 },
  { name: "idx_roomId_notDeleted_timestamp" }
)
```

## 샤딩 전략

### 샤드 키 설정
```javascript
// roomId 기반 샤딩 (방별 데이터 지역성 확보)
sh.shardCollection("vibechat_messages.messages", { "roomId": 1 })
```

### 샤딩 고려사항
- **장점**: 방별 데이터 지역성, 쿼리 성능 최적화
- **단점**: 인기 방에 핫스팟 발생 가능
- **대안**: roomId + timestamp 해시 기반 샤딩 (향후 검토)

## 데이터 타입별 상세 스키마

### TEXT 메시지
```javascript
{
  "type": "TEXT",
  "content": {
    "text": "안녕하세요!",
    "media": null,
    "system": null
  }
}
```

### 미디어 메시지
```javascript
{
  "type": "IMAGE",
  "content": {
    "text": "사진 설명",  // 선택적
    "media": {
      "url": "https://s3.../image.jpg",
      "thumbnailUrl": "https://s3.../thumb.jpg",
      "mimeType": "image/jpeg",
      "size": 1024000,
      "width": 1920,
      "height": 1080,
      "metadata": {
        "exif": {...},
        "processed": true
      }
    },
    "system": null
  }
}
```

### 시스템 메시지
```javascript
{
  "type": "SYSTEM",
  "content": {
    "text": null,
    "media": null,
    "system": {
      "action": "USER_JOINED",
      "data": {
        "targetUserId": 1234,
        "targetNickname": "newuser"
      }
    }
  }
}
```

## 성능 최적화 전략

### 1. 읽기 최적화
- userInfo 비정규화로 JOIN 제거
- 페이지네이션 최적화 (cursor 기반)
- 적절한 인덱스 힌트 사용

### 2. 쓰기 최적화
- Bulk Insert 활용
- Write Concern 조정 (w:1, j:false)
- Connection Pool 크기 최적화

### 3. 저장소 최적화
- WiredTiger 압축 (snappy)
- TTL 인덱스 활용 (임시 데이터)
- 적절한 청크 크기 설정

## 마이그레이션 계획

### Phase 1: 인프라 구성
- MongoDB 클러스터 구성 (3-node replica set)
- 모니터링 및 백업 시스템 구축
- 성능 테스트 환경 구성

### Phase 2: 스키마 구현
- 컬렉션 및 인덱스 생성
- 애플리케이션 코드 구현
- 단위 테스트 및 통합 테스트

### Phase 3: 데이터 마이그레이션
- 기존 MySQL 데이터 변환 스크립트
- 점진적 마이그레이션 (Blue-Green)
- 데이터 일관성 검증

## 모니터링 지표

### 성능 지표
- 쿼리 응답 시간 (P95 < 100ms)
- 초당 처리량 (TPS)
- 인덱스 효율성 (Index Hit Ratio)

### 용량 지표
- 컬렉션 크기 증가율
- 인덱스 크기 비율
- 샤드별 데이터 분산도

### 가용성 지표
- Replica Set 상태
- 샤드 클러스터 건강도
- 백업 성공률

## 보안 고려사항

### 데이터 보호
- 필드 레벨 암호화 (민감 정보)
- 접근 권한 관리 (RBAC)
- 감사 로그 활성화

### 네트워크 보안
- TLS/SSL 통신 암호화
- VPC 내부 네트워크 격리
- 방화벽 규칙 설정

## 향후 확장 계획

### 기능 확장
- 메시지 스레드 (답글) 지원
- 메시지 번역 기능
- AI 기반 메시지 분석

### 기술적 확장
- 검색 엔진 연동 (Elasticsearch)
- CDC를 통한 실시간 분석
- 아카이빙 시스템 구축

## 문서 버전 이력

| 버전 | 날짜 | 변경사항 | 작성자 |
|------|------|----------|--------|
| 1.0 | 2024-09-23 | 초기 스키마 설계 | DBA Team |

## 검토 및 승인

- [ ] Backend Team Lead 검토
- [ ] DevOps Team 검토
- [ ] Security Team 검토
- [ ] CTO 승인