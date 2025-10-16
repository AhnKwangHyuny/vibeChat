# 방 목록 조회 쿼리 최적화 가이드

## 📋 목차
1. [아키텍처 개요](#아키텍처-개요)
2. [최적화 전략](#최적화-전략)
3. [성능 분석](#성능-분석)
4. [확장 계획](#확장-계획)

---

## 🏗️ 아키텍처 개요

### **Repository 구조**

```
ChatRoomRepository (Interface)
├── JpaRepository<ChatRoom, Long>
└── ChatRoomRepositoryCustom
    └── ChatRoomRepositoryImpl (구현체)
```

### **주요 메서드**

| 메서드 | 용도 | N+1 해결 | 페이지네이션 |
|--------|------|----------|--------------|
| `findRoomsWithFilters()` | 동적 검색/필터 | ✅ Fetch Join | ✅ 커서 기반 |
| `findByTagsWithCursor()` | 태그 검색 | ✅ Fetch Join | ✅ 커서 기반 |
| `findRecentRoomsWithFetchJoin()` | 최신 방 목록 | ✅ Fetch Join | ❌ Limit만 |
| `findByRoomTags_Tag_NameIn()` | 레거시 (Deprecated) | ❌ N+1 발생 | ❌ |

---

## ⚡ 최적화 전략

### **1. N+1 문제 완전 해결**

#### **문제 상황**
```java
// ❌ N+1 발생 (4번의 추가 쿼리)
List<ChatRoom> rooms = chatRoomRepository.findAll();
for (ChatRoom room : rooms) {
    room.getRoomTags();      // +1 쿼리
    room.getCreatedBy();     // +1 쿼리
    // ...
}
```

#### **해결 방법**
```java
// ✅ 한 번의 쿼리로 모든 연관 데이터 로드
@Query("SELECT DISTINCT c FROM ChatRoom c " +
       "LEFT JOIN FETCH c.roomTags rt " +
       "LEFT JOIN FETCH rt.tag " +
       "LEFT JOIN FETCH c.createdBy " +
       "WHERE ...")
```

**생성되는 SQL:**
```sql
SELECT DISTINCT 
  c.*, 
  rt.*, 
  t.*, 
  u.*
FROM chat_rooms c
LEFT JOIN room_tags rt ON c.id = rt.room_id
LEFT JOIN tags t ON rt.tag_id = t.id
LEFT JOIN users u ON c.created_by = u.id
WHERE ...
```

---

### **2. 커서 기반 페이지네이션**

#### **Offset 기반 (비효율)**
```sql
-- ❌ 1000개를 건너뛰며 스캔 (느림)
SELECT * FROM chat_rooms 
ORDER BY created_at DESC 
LIMIT 20 OFFSET 1000;
```

**성능:**
- 10,000건: ~50ms
- 100,000건: ~500ms ⚠️
- 1,000,000건: ~5000ms ❌

#### **커서 기반 (효율)**
```sql
-- ✅ 인덱스 활용 (빠름)
SELECT * FROM chat_rooms 
WHERE id < :lastId 
ORDER BY created_at DESC, id DESC 
LIMIT 20;
```

**성능:**
- 10,000건: ~5ms ✅
- 100,000건: ~10ms ✅
- 1,000,000건: ~15ms ✅

**인덱스:**
```sql
CREATE INDEX idx_chatrooms_cursor_latest 
ON chat_rooms (created_at DESC, id DESC);
```

---

### **3. 태그 필터링 최적화**

#### **방법 A: EXISTS 서브쿼리 (추천)**
```java
@Query("SELECT c FROM ChatRoom c " +
       "WHERE EXISTS (" +
       "  SELECT 1 FROM RoomTag rt " +
       "  WHERE rt.chatRoom = c " +
       "  AND rt.tag.name IN :tagNames" +
       ")")
```

**장점:**
- ✅ 중복 제거 불필요
- ✅ 쿼리 플랜 최적화 가능
- ✅ 인덱스 활용 효율적

#### **방법 B: JOIN + DISTINCT (비추천)**
```java
@Query("SELECT DISTINCT c FROM ChatRoom c " +
       "JOIN c.roomTags rt " +
       "WHERE rt.tag.name IN :tagNames")
```

**단점:**
- ❌ 태그 개수만큼 중복 발생
- ❌ DISTINCT 처리 오버헤드
- ❌ 메모리 사용량 증가

---

### **4. 동적 쿼리 구성**

#### **요구사항 조합**
| 조건 | 필수 여부 | 동적 처리 |
|------|-----------|-----------|
| 커서 (lastId) | 선택적 | ✅ |
| 검색어 (query) | 선택적 | ✅ |
| 태그 (tags) | 선택적 | ✅ |
| 공개/비공개 | 선택적 | ✅ |
| 정렬 기준 | 필수 (기본값) | ✅ |

#### **구현 예시**
```java
StringBuilder jpql = new StringBuilder();
jpql.append("SELECT DISTINCT c FROM ChatRoom c ");
jpql.append("LEFT JOIN FETCH c.roomTags rt ");
jpql.append("WHERE 1=1 ");

if (request.getLastId() != null) {
    jpql.append("AND c.id < :lastId ");
}

if (request.getQuery() != null) {
    jpql.append("AND LOWER(c.title) LIKE LOWER(:query) ");
}
// ... 동적으로 조건 추가
```

---

## 📊 성능 분석

### **쿼리 실행 계획 분석**

#### **1. 최신순 커서 페이지네이션**
```sql
EXPLAIN SELECT * FROM chat_rooms 
WHERE id < 12345 
ORDER BY created_at DESC, id DESC 
LIMIT 20;
```

**예상 결과:**
```
+----+-------+------+--------------------------------+
| id | type  | key  | Extra                          |
+----+-------+------+--------------------------------+
|  1 | range | idx_chatrooms_cursor_latest    |
|    |       |      | Using where; Using index       |
+----+-------+------+--------------------------------+
```

#### **2. 태그 필터 쿼리**
```sql
EXPLAIN SELECT DISTINCT c.* FROM chat_rooms c
WHERE EXISTS (
  SELECT 1 FROM room_tags rt 
  WHERE rt.room_id = c.id 
  AND rt.tag_id IN (1, 2, 3)
)
ORDER BY c.created_at DESC
LIMIT 20;
```

**예상 결과:**
```
+----+-----------+------+---------------------------+
| id | type      | key  | Extra                     |
+----+-----------+------+---------------------------+
|  1 | ALL       | NULL | Using where; Using filesort |
|  2 | ref       | idx_roomtags_tag_room     |
|    |           |      | Using index               |
+----+-----------+------+---------------------------+
```

### **성능 벤치마크 (예상)**

| 데이터 규모 | 조건 | 응답 시간 | 인덱스 사용 |
|-------------|------|-----------|-------------|
| 1,000건 | 최신순 | ~5ms | ✅ |
| 10,000건 | 최신순 | ~10ms | ✅ |
| 100,000건 | 최신순 | ~15ms | ✅ |
| 100,000건 | 태그 필터 | ~30ms | ✅ |
| 100,000건 | 검색 + 필터 | ~50ms | ✅ |

---

## 🚀 확장 계획

### **Phase 1: 현재 구현 (완료)**
- ✅ 커서 기반 페이지네이션
- ✅ Fetch Join으로 N+1 해결
- ✅ 동적 필터 (검색, 태그, 공개/비공개)
- ✅ 최신순/오래된순/제목순 정렬

### **Phase 2: 인원순 정렬 (향후)**

#### **Option A: 비정규화 (추천)**
```sql
ALTER TABLE chat_rooms 
ADD COLUMN participants_count INT DEFAULT 0;

CREATE INDEX idx_chatrooms_participants_cursor 
ON chat_rooms (participants_count DESC, id DESC);
```

**장점:**
- ✅ 쿼리 성능 최고
- ✅ 정렬 간단

**단점:**
- ❌ 동기화 관리 필요
- ❌ 데이터 일관성 이슈

#### **Option B: Redis 기반 (권장)**
```java
// Service Layer
int participantsCount = roomParticipantService.getParticipantCount(roomId);
```

**장점:**
- ✅ 실시간 정확성
- ✅ 데이터 일관성

**단점:**
- ❌ DB 정렬 불가 (애플리케이션 레벨 정렬)
- ❌ 대량 데이터 시 메모리 사용

### **Phase 3: 전체 텍스트 검색 (향후)**

#### **MySQL Full-Text Search**
```sql
CREATE FULLTEXT INDEX idx_chatrooms_search_fulltext 
ON chat_rooms (title, description);

SELECT * FROM chat_rooms
WHERE MATCH(title, description) 
AGAINST('keyword' IN NATURAL LANGUAGE MODE);
```

#### **Elasticsearch 통합 (대규모)**
```java
@Document(indexName = "chat_rooms")
public class ChatRoomDocument {
    @Id private String id;
    @Field(type = Text) private String title;
    @Field(type = Text) private String description;
    // ...
}
```

---

## 🔧 운영 가이드

### **인덱스 모니터링**
```sql
-- 인덱스 크기 확인
SELECT 
  table_name,
  index_name,
  ROUND(stat_value * @@innodb_page_size / 1024 / 1024, 2) AS size_mb
FROM mysql.innodb_index_stats
WHERE table_name = 'chat_rooms'
ORDER BY size_mb DESC;
```

### **슬로우 쿼리 분석**
```sql
-- 슬로우 쿼리 로그 활성화
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1; -- 1초 이상

-- 로그 확인
tail -f /var/log/mysql/mysql-slow.log
```

### **쿼리 튜닝 체크리스트**
- [ ] EXPLAIN 실행 계획 확인
- [ ] 인덱스 사용 여부 확인 (key 컬럼)
- [ ] Using filesort 제거 (ORDER BY 최적화)
- [ ] Using temporary 제거 (DISTINCT 최적화)
- [ ] Fetch Join으로 N+1 제거
- [ ] 커서 페이지네이션 적용

---

## 📚 참고 자료

1. **Spring Data JPA Query Methods**
   - [공식 문서](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)

2. **MySQL 인덱스 최적화**
   - [High Performance MySQL](https://www.oreilly.com/library/view/high-performance-mysql/9781449332471/)

3. **JPA N+1 문제 해결**
   - [Hibernate Best Practices](https://vladmihalcea.com/n-plus-1-query-problem/)

