# ERD.md: 바이브 챗 엔티티-관계 다이어그램

이 문서는 기술 요구사항 문서(TRD)를 기반으로 바이브 챗 애플리케이션의 데이터베이스 스키마를 설명합니다.

## 데이터 모델 (텍스트 ERD)

- **`users` (사용자)**
  - `id` (BIGINT, PK, AI)
  - `provider` (ENUM('GOOGLE','GUEST'))
  - `provider_id` (VARCHAR(128), GUEST의 경우 null 허용, 인덱스)
  - `nickname` (VARCHAR(32), 전역 고유 UNIQUE INDEX)
  - `avatar_url` (VARCHAR(255), null 허용)
  - `greeting` (VARCHAR(160), null 허용)
  - `created_at` (DATETIME, 인덱스), `updated_at` (DATETIME)

- **`user_profile_tags` (사용자-프로필 태그 연결)**
  - `id` (BIGINT, PK, AI)
  - `user_id` (BIGINT, FK→users.id)
  - `tag_id` (BIGINT, FK→tags.id)
  - `created_at` (DATETIME)
  - UNIQUE(`user_id`,`tag_id`)

- **`user_rooms` (사용자-방 연결/북마크 포함)**
  - `id` (BIGINT, PK, AI)
  - `user_id` (BIGINT, FK→users.id)
  - `room_id` (BIGINT, FK→chat_rooms.id)
  - `bookmarked` (TINYINT(1) DEFAULT 0)
  - `joined_at` (DATETIME)
  - UNIQUE(`user_id`,`room_id`)

- **`chat_rooms` (채팅방)**
  - `id` (BIGINT, PK, AI)
  - `title` (VARCHAR(80), 인덱스)
  - `description` (VARCHAR(255), null 허용)
  - `is_private` (TINYINT(1), 인덱스)
  - `invite_code` (CHAR(36), 고유, 공개 방의 경우 null 허용)
  - `created_by` (BIGINT, FK→users.id, 인덱스)
  - `created_at` (DATETIME, 인덱스), `updated_at` (DATETIME)

- **`tags` (태그)**
  - `id` (BIGINT, PK, AI)
  - `name` (VARCHAR(32), 고유, 인덱스(접두사))
  - `popularity` (INT, 기본값 0, 인덱스)

- **`room_tags` (방-태그 연결)**
  - `room_id` (BIGINT, FK→chat_rooms.id)
  - `tag_id` (BIGINT, FK→tags.id)
  - PK(`room_id`, `tag_id`), 인덱스(`tag_id`)

- **`messages` (메시지)**
  - `id` (BIGINT, PK, AI)
  - `room_id` (BIGINT, FK→chat_rooms.id, 인덱스)
  - `user_id` (BIGINT, FK→users.id, 인덱스)
  - `type` (ENUM: TEXT, IMAGE, GIF, VIDEO, 인덱스)
  - `content_text` (TEXT, null 허용)
  - `media_url` (VARCHAR(255), null 허용)
  - `media_thumb_url` (VARCHAR(255), null 허용)
  - `media_duration_sec` (SMALLINT, null 허용)
  - `client_temp_id` (VARCHAR(36), null 허용, 인덱스)
  - `is_deleted` (TINYINT(1), 기본값 0)
  - `created_at` (DATETIME, 인덱스 DESC)

- **`message_reports` (메시지 신고)**
  - `id` (BIGINT, PK, AI)
  - `message_id` (BIGINT, FK→messages.id, 인덱스)
  - `reporter_user_id` (BIGINT, FK→users.id, null 허용)
  - `reason` (VARCHAR(64))
  - `details` (VARCHAR(255), null 허용)
  - `created_at` (DATETIME)

## 인덱싱 전략

- `messages`(`room_id`, `created_at` DESC) 커버링 인덱스
- `room_tags`(`room_id`, `tag_id`), `room_tags`(`tag_id`)
- `tags`(`name` 접두사 인덱스), `tags`(`popularity`)
- `chat_rooms`(`is_private`, `created_at`), `chat_rooms`(`title`)

## 보존 정책 (하우스키핑)

- 방당 최신 메시지 1,000개 유지. `created_at` DESC를 기준으로 cron(스케줄러)을 통해 방당 정리.
- 미디어 파일은 메시지 삭제 시 소프트 삭제됩니다(데모 범위 내).