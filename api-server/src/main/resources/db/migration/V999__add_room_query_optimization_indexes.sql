-- =============================================
-- 방 목록 조회 쿼리 최적화를 위한 인덱스
-- =============================================
-- 작성자: Tech Lead
-- 목적: 커서 기반 페이지네이션 및 다양한 정렬 옵션 지원
-- 성능 목표: 100만 건 이상에서도 sub-100ms 응답

-- =============================================
-- 1. 커서 기반 페이지네이션 최적화
-- =============================================

-- 최신순 정렬 (기본, 가장 많이 사용)
-- WHERE id < :lastId ORDER BY created_at DESC, id DESC
CREATE INDEX idx_chatrooms_cursor_latest 
ON chat_rooms (created_at DESC, id DESC);

-- 오래된 순 정렬
-- WHERE id > :lastId ORDER BY created_at ASC, id ASC
CREATE INDEX idx_chatrooms_cursor_oldest 
ON chat_rooms (created_at ASC, id ASC);

-- 제목순 정렬 (알파벳)
-- WHERE id < :lastId ORDER BY title ASC, id DESC
CREATE INDEX idx_chatrooms_cursor_title 
ON chat_rooms (title ASC, id DESC);

-- =============================================
-- 2. 검색 쿼리 최적화
-- =============================================

-- 제목 검색 (LIKE '%keyword%')
-- Full-text search index (MySQL 5.7+)
CREATE FULLTEXT INDEX idx_chatrooms_title_fulltext 
ON chat_rooms (title);

-- 설명 검색 (선택적)
CREATE FULLTEXT INDEX idx_chatrooms_description_fulltext 
ON chat_rooms (description);

-- 제목 + 설명 복합 검색 (가장 효율적)
CREATE FULLTEXT INDEX idx_chatrooms_search_fulltext 
ON chat_rooms (title, description);

-- =============================================
-- 3. 필터링 쿼리 최적화
-- =============================================

-- 공개/비공개 필터
-- WHERE is_private = ? AND id < :lastId ORDER BY created_at DESC
CREATE INDEX idx_chatrooms_private_cursor 
ON chat_rooms (is_private, created_at DESC, id DESC);

-- 생성자 기반 검색 (향후 "내가 만든 방" 필터용)
-- WHERE created_by = ? ORDER BY created_at DESC
CREATE INDEX idx_chatrooms_creator_date 
ON chat_rooms (created_by, created_at DESC);

-- =============================================
-- 4. 태그 기반 검색 최적화
-- =============================================

-- room_tags 테이블 복합 인덱스
-- EXISTS (SELECT 1 FROM room_tags WHERE room_id = ? AND tag_id IN (?))
CREATE INDEX idx_roomtags_tag_room 
ON room_tags (tag_id, room_id);

-- 역방향 조회용 (방 → 태그)
CREATE INDEX idx_roomtags_room_tag 
ON room_tags (room_id, tag_id);

-- =============================================
-- 5. 인원순 정렬 (향후 구현)
-- =============================================

-- Option A: participants_count 컬럼 추가 (비정규화)
-- ALTER TABLE chat_rooms ADD COLUMN participants_count INT DEFAULT 0;
-- CREATE INDEX idx_chatrooms_participants_cursor 
-- ON chat_rooms (participants_count DESC, id DESC);

-- Option B: 서브쿼리 최적화 (정규화 유지)
-- (SELECT COUNT(*) FROM room_participants WHERE room_id = ?)
-- → Redis 기반 실시간 카운트 권장

-- =============================================
-- 성능 분석 가이드
-- =============================================

/*
EXPLAIN 실행 예시:

1. 최신순 커서 페이지네이션:
EXPLAIN SELECT * FROM chat_rooms 
WHERE id < 12345 
ORDER BY created_at DESC, id DESC 
LIMIT 20;
→ idx_chatrooms_cursor_latest 사용 확인

2. 태그 필터 + 커서:
EXPLAIN SELECT DISTINCT c.* FROM chat_rooms c
WHERE EXISTS (
  SELECT 1 FROM room_tags rt 
  WHERE rt.room_id = c.id 
  AND rt.tag_id IN (1, 2, 3)
)
AND c.id < 12345
ORDER BY c.created_at DESC, c.id DESC
LIMIT 20;
→ idx_roomtags_tag_room 사용 확인

3. 검색 + 필터:
EXPLAIN SELECT * FROM chat_rooms
WHERE (
  MATCH(title, description) AGAINST('keyword' IN NATURAL LANGUAGE MODE)
)
AND is_private = false
AND id < 12345
ORDER BY created_at DESC, id DESC
LIMIT 20;
→ idx_chatrooms_search_fulltext 사용 확인
*/

-- =============================================
-- 인덱스 크기 모니터링 쿼리
-- =============================================

/*
SELECT 
  table_name,
  index_name,
  ROUND(stat_value * @@innodb_page_size / 1024 / 1024, 2) AS size_mb
FROM mysql.innodb_index_stats
WHERE table_name = 'chat_rooms'
  AND stat_name = 'size'
ORDER BY size_mb DESC;
*/

