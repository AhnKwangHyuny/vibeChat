# 🏗️ VibeChat Database Architecture

## 📊 현재 엔티티 구조 분석 및 최적화 설계

### **현재 시스템 분석 결과**
- **중복 엔티티**: UserRoom vs ChatRoomParticipant 동일 목적 중복
- **성능 이슈**: User.nickname 글로벌 유니크로 방별 중복 불가
- **확장성 한계**: Message.contentText @Lob로 모든 조회에 포함
- **비즈니스 로직 불일치**: 게스트 사용자 방별 닉네임 시스템 필요

---

## 🎯 **핵심 3축 도메인 관계 매핑**

```
🏢 핵심 도메인 관계
├─ User (1) ──────→ (N) Message [사용자가 보낸 메시지들]
├─ ChatRoom (1) ──→ (N) Message [방의 모든 메시지들]
├─ User (M) ←────→ (N) ChatRoom [참여한 방들 - via RoomParticipants]
└─ User (1) ──────→ (N) ChatRoom [createdBy: 개설한 방들]

🏷️ 태그 시스템
├─ Tag (1) ←─────→ (N) RoomTag ←─────→ (1) ChatRoom [방-태그 다대다]
└─ Tag: {name(unique), usage_count, trending_score, category}

🔐 참여 관리 (통합 설계)
└─ RoomParticipants: {room_id, user_id, room_nickname, status, role, read_status}

🚨 부가 기능
├─ MessageReport: {message_id, reporter_user_id, reason, details}
├─ MessageReadStatus: {message_id, user_id, read_at} [카카오톡 스타일]
└─ UserSessions: {session_id, user_id, room_id, heartbeat} [실시간 관리]
```

---

## 🚀 **최적화된 DB 스키마 설계**

### **1. User 엔티티 최적화**

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider ENUM('GUEST', 'GOOGLE') NOT NULL,
    provider_id VARCHAR(128) NULL COMMENT 'OAuth ID',
    global_nickname VARCHAR(32) NOT NULL COMMENT '전역 표시명',
    avatar_url VARCHAR(255) NULL,
    greeting VARCHAR(160) NULL,

    -- 🆕 게스트 사용자 관리
    is_guest BOOLEAN NOT NULL DEFAULT FALSE,
    guest_session_id VARCHAR(64) NULL COMMENT '게스트 세션 추적',

    -- 🆕 사용자 상태 관리
    last_active_at DATETIME NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') DEFAULT 'ACTIVE',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 🚀 성능 최적화 인덱스
    INDEX idx_provider_provider_id (provider, provider_id),
    INDEX idx_guest_session (guest_session_id),
    INDEX idx_last_active (last_active_at),
    INDEX idx_status (status)

) COMMENT='사용자 정보 - 글로벌 닉네임 제약 제거로 방별 중복 허용';

-- ❌ 제거: UNIQUE KEY uk_nickname (nickname) - 방별 중복 허용을 위해 제거
```

### **2. ChatRoom 엔티티 최적화**

```sql
CREATE TABLE chat_rooms (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(80) NOT NULL,
    description VARCHAR(255) NULL,
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    invite_code CHAR(36) NULL COMMENT 'UUID format',

    created_by BIGINT NOT NULL,

    -- 🆕 방 관리 필드 추가
    max_participants INT DEFAULT 100,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at DATETIME NULL,

    -- 🆕 통계 필드 (성능 캐시용)
    total_messages_count INT NOT NULL DEFAULT 0,
    active_participants_count INT NOT NULL DEFAULT 0,
    last_message_at DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (created_by) REFERENCES users(id),
    UNIQUE KEY uk_invite_code (invite_code),

    -- 🚀 성능 최적화 인덱스
    INDEX idx_is_private_created_at (is_private, created_at DESC),
    INDEX idx_created_by (created_by),
    INDEX idx_last_message_at (last_message_at DESC),
    INDEX idx_title_fulltext (title),
    INDEX idx_status_activity (is_archived, last_message_at DESC)

) COMMENT='채팅방 정보 - 통계 필드로 성능 최적화';
```

### **3. Message 엔티티 대폭 최적화**

```sql
-- 핵심 메시지 테이블 (대용량 텍스트 분리)
CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    -- 🔄 메시지 타입 및 상태 관리
    type ENUM('TEXT', 'IMAGE', 'GIF', 'VIDEO', 'FILE', 'SYSTEM') NOT NULL,
    status ENUM('PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED') DEFAULT 'SENT',

    -- 🆕 메시지 순서 및 식별 (핵심 개선)
    client_temp_id CHAR(36) NULL COMMENT '클라이언트 임시 ID',
    message_seq BIGINT NOT NULL COMMENT '방 내 메시지 순서 보장',
    reply_to_message_id BIGINT NULL COMMENT '답글 기능',

    -- 🔄 미디어 정보 확장
    media_url VARCHAR(500) NULL,
    media_thumb_url VARCHAR(500) NULL,
    media_duration_sec SMALLINT NULL,
    media_file_size BIGINT NULL COMMENT '파일 크기 (bytes)',
    media_mime_type VARCHAR(100) NULL COMMENT 'MIME 타입',

    -- 🆕 메시지 메타데이터
    edited_at DATETIME NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (reply_to_message_id) REFERENCES messages(id),

    -- 🚀 핵심 성능 인덱스 (실시간 채팅 최적화)
    INDEX idx_room_seq_coverage (room_id, message_seq DESC, user_id, type, created_at),
    INDEX idx_room_created (room_id, created_at DESC),
    INDEX idx_client_temp_id (client_temp_id),
    INDEX idx_user_created (user_id, created_at DESC),
    INDEX idx_reply_chain (reply_to_message_id),

    -- 🆕 메시지 순서 보장을 위한 유니크 제약
    UNIQUE KEY uk_room_message_seq (room_id, message_seq)

) COMMENT='메시지 정보 - 대용량 텍스트 분리 및 순서 보장'
-- 🚀 월별 파티셔닝 (대용량 처리)
PARTITION BY RANGE (YEAR(created_at) * 100 + MONTH(created_at)) (
    PARTITION p202501 VALUES LESS THAN (202502),
    PARTITION p202502 VALUES LESS THAN (202503),
    PARTITION p202503 VALUES LESS THAN (202504),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 🆕 메시지 내용 분리 테이블 (성능 최적화)
CREATE TABLE message_contents (
    message_id BIGINT PRIMARY KEY,
    content_text MEDIUMTEXT NOT NULL COMMENT '대용량 텍스트 내용',

    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,

    -- 전문 검색 인덱스
    FULLTEXT INDEX ft_content_text (content_text)

) COMMENT='메시지 텍스트 내용 - 조회 성능 최적화를 위해 분리';
```

### **4. 통합된 방 참여 관리 (핵심 개선)**

```sql
-- ❌ 기존: ChatRoomParticipant + UserRoom 중복
-- ✅ 신규: 통합된 참여 관리 테이블

CREATE TABLE room_participants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    -- 🔄 방별 닉네임 (핵심 비즈니스 로직)
    room_nickname VARCHAR(32) NOT NULL COMMENT '방 내에서만 유니크한 닉네임',

    -- 🆕 참여 상태 관리
    status ENUM('ACTIVE', 'LEFT', 'KICKED', 'BANNED') DEFAULT 'ACTIVE',
    role ENUM('OWNER', 'ADMIN', 'MEMBER') DEFAULT 'MEMBER',

    -- 🆕 개인화 설정
    is_bookmarked BOOLEAN NOT NULL DEFAULT FALSE,
    is_muted BOOLEAN NOT NULL DEFAULT FALSE,
    notification_enabled BOOLEAN NOT NULL DEFAULT TRUE,

    -- 🆕 읽음 상태 관리 (카카오톡 스타일)
    last_read_message_seq BIGINT NULL COMMENT '마지막 읽은 메시지 순서',
    last_read_at DATETIME NULL,

    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at DATETIME NULL,

    FOREIGN KEY (room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id),

    -- 🚀 핵심 제약 및 인덱스
    UNIQUE KEY uk_room_user (room_id, user_id),
    UNIQUE KEY uk_room_nickname (room_id, room_nickname) COMMENT '방별 닉네임 유니크',

    INDEX idx_user_active (user_id, status),
    INDEX idx_room_active (room_id, status),
    INDEX idx_joined_at (joined_at),
    INDEX idx_read_status (room_id, last_read_message_seq)

) COMMENT='방 참여자 관리 - UserRoom과 ChatRoomParticipant 통합, 방별 닉네임 지원';
```

### **5. 태그 시스템 최적화**

```sql
CREATE TABLE tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(32) NOT NULL UNIQUE,

    -- 🆕 태그 관리 필드
    category ENUM('TECH', 'HOBBY', 'STUDY', 'GAME', 'MUSIC', 'OTHER') DEFAULT 'OTHER',
    usage_count INT NOT NULL DEFAULT 0 COMMENT '사용 횟수',
    trending_score DECIMAL(10,4) DEFAULT 0.0 COMMENT '트렌딩 점수 (알고리즘 기반)',

    is_featured BOOLEAN NOT NULL DEFAULT FALSE COMMENT '추천 태그',
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE COMMENT '차단된 태그',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 🚀 검색 최적화 인덱스
    INDEX idx_name_prefix (name(10)) COMMENT '자동완성용 prefix 인덱스',
    INDEX idx_trending (trending_score DESC, usage_count DESC) COMMENT '인기 태그',
    INDEX idx_category (category, usage_count DESC),
    INDEX idx_featured (is_featured, trending_score DESC),
    INDEX idx_status (is_blocked)

) COMMENT='태그 정보 - 트렌딩 시스템 및 카테고리 분류';

-- RoomTag 최적화 (기존 구조 유지하되 인덱스 강화)
CREATE TABLE room_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id),

    UNIQUE KEY uk_room_tag (room_id, tag_id),
    INDEX idx_tag_room (tag_id, room_id) COMMENT '태그별 방 검색',
    INDEX idx_created_at (created_at)

) COMMENT='방-태그 관계 - 검색 최적화';
```

---

## 🚀 **신규 필요 엔티티**

### **6. 메시지 읽음 상태 추적 (카카오톡 스타일)**

```sql
-- 대용량 읽음 상태 최적화
CREATE TABLE message_read_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id),

    UNIQUE KEY uk_message_user (message_id, user_id),
    INDEX idx_user_read_at (user_id, read_at DESC),
    INDEX idx_message_read_count (message_id)

) COMMENT='메시지 읽음 상태 - 카카오톡 스타일 읽음 확인'
-- 월별 파티셔닝 (메시지와 동일)
PARTITION BY RANGE (YEAR(read_at) * 100 + MONTH(read_at));
```

### **7. 실시간 세션 관리**

```sql
-- Redis 보완용 DB 세션 백업
CREATE TABLE user_sessions (
    id VARCHAR(64) PRIMARY KEY COMMENT 'STOMP session ID',
    user_id BIGINT NOT NULL,
    room_id BIGINT NULL COMMENT '현재 접속 방',

    device_info JSON NULL COMMENT '브라우저/디바이스 정보',
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,

    connected_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_heartbeat_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    disconnected_at DATETIME NULL,

    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (room_id) REFERENCES chat_rooms(id),

    INDEX idx_user_connected (user_id, connected_at),
    INDEX idx_room_active (room_id, disconnected_at),
    INDEX idx_heartbeat (last_heartbeat_at),
    INDEX idx_active_sessions (disconnected_at, last_heartbeat_at)

) COMMENT='실시간 세션 관리 - Redis 백업 및 분석용';
```

### **8. 메시지 처리 상태 (Redis Streams 백업)**

```sql
-- 메시지 큐 처리 상태 추적
CREATE TABLE message_processing_status (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_temp_id CHAR(36) NOT NULL UNIQUE,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    status ENUM('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED') DEFAULT 'QUEUED',
    redis_stream_id VARCHAR(64) NULL COMMENT 'Redis Streams ID',

    message_id BIGINT NULL COMMENT '생성된 실제 메시지 ID',
    error_reason TEXT NULL COMMENT '실패 사유',
    retry_count INT NOT NULL DEFAULT 0,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,

    FOREIGN KEY (room_id) REFERENCES chat_rooms(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (message_id) REFERENCES messages(id),

    INDEX idx_status_created (status, created_at),
    INDEX idx_room_user (room_id, user_id),
    INDEX idx_client_temp_id (client_temp_id),
    INDEX idx_retry_failed (status, retry_count)

) COMMENT='메시지 처리 상태 추적 - Redis Streams 백업';
```

### **9. 메시지 순서 보장 시스템**

```sql
-- 방별 메시지 시퀀스 관리
CREATE TABLE room_message_sequences (
    room_id BIGINT PRIMARY KEY,
    current_seq BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE

) COMMENT='방별 메시지 순서 보장을 위한 시퀀스 관리';

-- 메시지 삽입 시 시퀀스 증가 트리거
DELIMITER $$
CREATE TRIGGER tr_message_sequence
BEFORE INSERT ON messages
FOR EACH ROW
BEGIN
    DECLARE next_seq BIGINT;

    -- 방별 다음 시퀀스 번호 획득 (원자적 연산)
    INSERT INTO room_message_sequences (room_id, current_seq)
    VALUES (NEW.room_id, 1)
    ON DUPLICATE KEY UPDATE current_seq = current_seq + 1;

    SELECT current_seq INTO next_seq
    FROM room_message_sequences
    WHERE room_id = NEW.room_id;

    SET NEW.message_seq = next_seq;
END$$
DELIMITER ;
```

---

## 🗑️ **제거할 엔티티들**

### **❌ 삭제 대상 및 이유**

1. **ChatRoomParticipant** → `room_participants`로 통합
   - 중복 기능으로 데이터 일관성 문제
   - 불필요한 JOIN 연산 증가

2. **UserRoom** → `room_participants`로 통합
   - ChatRoomParticipant와 동일 목적
   - 북마크 기능은 `room_participants.is_bookmarked`로 이관

3. **UserProfileTag** → MVP에서 제외
   - 현재 미사용 상태
   - 방 태그가 더 중요한 기능

---

## 📈 **핵심 성능 최적화 전략**

### **1. 실시간 채팅 조회 최적화**

```sql
-- 🚀 가장 빈번한 쿼리: 방별 최신 메시지 30개 조회
-- 목표: 50ms 이내 응답

-- 최적화된 커버링 인덱스
CREATE INDEX idx_messages_room_coverage
ON messages (room_id, is_deleted, message_seq DESC, user_id, type, created_at);

-- 최적화된 조회 쿼리 (message_seq 활용)
SELECT
    m.id, m.type, m.message_seq, m.user_id, m.created_at,
    m.media_url, m.media_thumb_url, m.media_duration_sec,
    u.global_nickname, u.avatar_url,
    mc.content_text
FROM messages m
JOIN users u ON m.user_id = u.id
LEFT JOIN message_contents mc ON m.id = mc.message_id
WHERE m.room_id = ?
  AND m.is_deleted = 0
ORDER BY m.message_seq DESC
LIMIT 30;
```

### **2. 태그 검색 최적화**

```sql
-- 🚀 태그 자동완성 최적화 (목표: 100ms 이내)

-- 최적화된 자동완성 쿼리
SELECT name, usage_count, trending_score, category
FROM tags
WHERE name LIKE CONCAT(?, '%')
  AND is_blocked = 0
ORDER BY
  is_featured DESC,
  trending_score DESC,
  usage_count DESC,
  LENGTH(name) ASC
LIMIT 10;

-- 트렌딩 태그 조회
SELECT name, trending_score, usage_count
FROM tags
WHERE is_blocked = 0
  AND usage_count > 0
ORDER BY trending_score DESC, usage_count DESC
LIMIT 20;
```

### **3. 방 검색 최적화**

```sql
-- 🚀 태그 기반 방 검색 최적화 (목표: 200ms 이내)

-- EXISTS 사용으로 조인 최적화
SELECT DISTINCT
    cr.id, cr.title, cr.description, cr.is_private,
    cr.active_participants_count, cr.last_message_at
FROM chat_rooms cr
WHERE cr.is_private = 0
  AND cr.is_archived = 0
  AND EXISTS (
    SELECT 1 FROM room_tags rt
    JOIN tags t ON rt.tag_id = t.id
    WHERE rt.room_id = cr.id
      AND t.name IN (?, ?, ?)
      AND t.is_blocked = 0
  )
ORDER BY cr.last_message_at DESC
LIMIT 20;
```

### **4. 읽음 상태 최적화**

```sql
-- 🚀 읽지 않은 메시지 수 조회 (목표: 150ms 이내)

-- 방별 읽지 않은 메시지 수
SELECT
    rp.room_id,
    cr.title,
    COUNT(m.id) as unread_count,
    MAX(m.message_seq) as latest_seq,
    rp.last_read_message_seq
FROM room_participants rp
JOIN chat_rooms cr ON rp.room_id = cr.id
LEFT JOIN messages m ON m.room_id = rp.room_id
  AND m.message_seq > COALESCE(rp.last_read_message_seq, 0)
  AND m.is_deleted = 0
WHERE rp.user_id = ?
  AND rp.status = 'ACTIVE'
GROUP BY rp.room_id, cr.title, rp.last_read_message_seq;

-- 읽음 상태 업데이트 (배치 처리)
UPDATE room_participants
SET last_read_message_seq = ?,
    last_read_at = NOW()
WHERE room_id = ? AND user_id = ?;
```

---

## 📊 **성능 모니터링 및 측정**

### **1. 쿼리 성능 목표**

| 쿼리 유형 | 목표 응답시간 | 최적화 방법 |
|----------|-------------|------------|
| 최신 메시지 30개 조회 | < 50ms | 커버링 인덱스 + message_seq |
| 태그 자동완성 | < 100ms | Prefix 인덱스 + 트렌딩 점수 |
| 방 검색 (태그 3개) | < 200ms | EXISTS + 복합 인덱스 |
| 읽지 않은 메시지 수 | < 150ms | 집계 인덱스 + 캐시 |
| 메시지 순서 할당 | < 10ms | 시퀀스 테이블 + 트리거 |

### **2. 인덱스 효율성 체크**

```sql
-- 인덱스 사용률 모니터링
SELECT
  table_name,
  index_name,
  cardinality,
  index_type,
  comment
FROM information_schema.statistics
WHERE table_schema = 'vibechat'
  AND table_name IN ('messages', 'room_participants', 'tags', 'chat_rooms')
ORDER BY table_name, seq_in_index;

-- 사용되지 않는 인덱스 찾기
SELECT
  object_schema,
  object_name,
  index_name,
  count_read,
  count_write
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE object_schema = 'vibechat'
  AND count_read = 0
  AND index_name IS NOT NULL
ORDER BY count_write DESC;
```

---

## 🔄 **마이그레이션 전략**

### **단계별 마이그레이션 계획**

#### **Phase 1: 스키마 준비 (무중단)**
```sql
-- 1-1. 새 테이블 생성
CREATE TABLE room_participants (...);
CREATE TABLE message_contents (...);
CREATE TABLE room_message_sequences (...);

-- 1-2. 새 인덱스 생성 (기존 테이블)
CREATE INDEX idx_messages_room_coverage ON messages (...);
CREATE INDEX idx_tags_name_prefix ON tags (name(10));

-- 1-3. 트리거 생성 (메시지 순서 보장)
CREATE TRIGGER tr_message_sequence ...;
```

#### **Phase 2: 데이터 마이그레이션**
```sql
-- 2-1. 참여자 데이터 통합
INSERT INTO room_participants (room_id, user_id, room_nickname, joined_at, is_bookmarked, status)
SELECT
    cp.chat_room_id,
    cp.user_id,
    u.global_nickname as room_nickname,
    cp.joined_at,
    COALESCE(ur.bookmarked, FALSE),
    'ACTIVE'
FROM chat_room_participants cp
JOIN users u ON cp.user_id = u.id
LEFT JOIN user_rooms ur ON cp.chat_room_id = ur.room_id AND cp.user_id = ur.user_id;

-- 2-2. 메시지 내용 분리
INSERT INTO message_contents (message_id, content_text)
SELECT id, contentText
FROM messages
WHERE contentText IS NOT NULL
  AND contentText != ''
  AND type = 'TEXT';

-- 2-3. 메시지 시퀀스 초기화
INSERT INTO room_message_sequences (room_id, current_seq)
SELECT room_id, COUNT(*) as current_seq
FROM messages
GROUP BY room_id;
```

#### **Phase 3: 스키마 정리**
```sql
-- 3-1. 기존 컬럼 제거
ALTER TABLE messages DROP COLUMN contentText;
ALTER TABLE users DROP INDEX uk_nickname;

-- 3-2. 사용하지 않는 테이블 DROP
DROP TABLE chat_room_participants;
DROP TABLE user_rooms;
DROP TABLE user_profile_tags;

-- 3-3. Foreign Key 재연결
ALTER TABLE messages ADD CONSTRAINT fk_messages_room_seq
FOREIGN KEY (room_id, message_seq) REFERENCES room_message_sequences(room_id);
```

---

## 🎯 **실무 적용 우선순위**

### **🚀 High Priority (즉시 적용 가능)**
1. **인덱스 최적화**
   - `idx_messages_room_coverage` 생성
   - `idx_tags_name_prefix` 생성

2. **닉네임 제약 완화**
   - `users.nickname` UNIQUE 제거
   - 방별 닉네임 시스템 준비

3. **메시지 조회 쿼리 최적화**
   - `message_seq` 컬럼 추가
   - 순서 보장 로직 구현

### **🔧 Medium Priority (Phase 1 완료 후)**
1. **테이블 통합**
   - `room_participants` 생성 및 마이그레이션
   - 중복 엔티티 제거

2. **메시지 내용 분리**
   - `message_contents` 테이블 분리
   - 조회 성능 최적화

3. **태그 시스템 고도화**
   - 트렌딩 점수 시스템
   - 카테고리 분류

### **📈 Low Priority (Phase 2-3)**
1. **파티셔닝 적용**
   - 메시지 월별 파티셔닝
   - 대용량 데이터 처리

2. **읽음 상태 시스템**
   - 카카오톡 스타일 구현
   - 실시간 읽음 확인

3. **고급 기능**
   - 답글 시스템
   - 메시지 편집/삭제
   - 세션 관리 테이블

---

## 📋 **요약 및 기대 효과**

### **핵심 개선사항**
1. **방별 닉네임 시스템**: 게스트 사용자 UX 개선
2. **메시지 순서 보장**: 실시간 채팅 안정성 확보
3. **성능 최적화**: 응답시간 50% 개선 목표
4. **확장성 확보**: 파티셔닝으로 대용량 처리 가능

### **비즈니스 가치**
- **사용자 경험**: 카카오톡 수준의 채팅 품질
- **확장성**: 동시 접속 100명 → 1000명 가능
- **개발 효율**: 중복 엔티티 제거로 유지보수성 향상
- **운영 안정성**: 모니터링 및 장애 복구 체계 구축

이러한 DB 아키텍처 최적화를 통해 **실시간 채팅 서비스의 핵심 성능과 안정성**을 확보할 수 있습니다.