# API.md: 바이브 챗 REST API 명세

이 문서는 기술 요구사항 문서(TRD)를 기반으로 바이브 챗 애플리케이션의 RESTful API 엔드포인트를 설명합니다.

## 공통

- **기본 URL:** `/api`
- **인증:** 세션 쿠키(게스트), OAuth2(Google - P1)
- **CSRF:** 쿠키/헤더 이중 제출
- **응답:** `application/json`, RFC7807 오류 형식 권장
- **페이징:** 커서 기반(beforeId 또는 beforeTimestamp), 제한(기본 30, 최대 50)

## 엔드포인트

### 사용자 관리

#### `POST /api/users/guest`

새로운 게스트 사용자를 생성하고 세션을 설정합니다.

- **요청 본문:**
  ```json
  {
    "nickname": "string"
  }
  ```
- **응답 (201 Created):**
  ```json
  {
    "userId": 123,
    "nickname": "GuestUser",
    "avatarUrl": null
  }
  ```
- **오류 (409 Conflict):** 닉네임이 이미 사용 중입니다.

### 채팅방 관리

#### `POST /api/rooms`

새로운 채팅방을 생성합니다.

- **요청 본문:**
  ```json
  {
    "title": "string",
    "description": "string (선택 사항)",
    "isPrivate": "boolean",
    "tags": ["string", "string"]
  }
  ```
- **응답 (201 Created):**
  ```json
  {
    "id": 1,
    "title": "나의 멋진 방",
    "description": "멋진 사람들을 위한 방.",
    "isPrivate": false,
    "tags": ["멋진", "채팅"],
    "participantsCount": 0,
    "lastMessageAt": null,
    "inviteCode": null
  }
  ```

#### `GET /api/rooms/{roomId}`

특정 채팅방의 상세 정보를 검색합니다.

- **경로 매개변수:**
  - `roomId`: `number` (채팅방 ID)
- **응답 (200 OK):**
  ```json
  {
    "id": 1,
    "title": "나의 멋진 방",
    "description": "멋진 사람들을 위한 방.",
    "isPrivate": false,
    "tags": ["멋진", "채팅"],
    "participantsCount": 5,
    "lastMessageAt": "2025-09-09T10:30:00",
    "inviteCode": "uuid-string-if-private"
  }
  ```
- **오류 (404 Not Found):** 채팅방을 찾을 수 없습니다.

#### `GET /api/rooms/search`

태그를 기반으로 채팅방을 검색합니다.

- **쿼리 매개변수:**
  - `tags`: `string[]` (쉼표로 구분된 태그 목록, 예: `?tags=tag1,tag2`)
- **응답 (200 OK):**
  ```json
  [
    {
      "id": 1,
      "title": "나의 멋진 방",
      "description": "멋진 사람들을 위한 방.",
      "isPrivate": false,
      "tags": ["멋진", "채팅"],
      "participantsCount": 5,
      "lastMessageAt": "2025-09-09T10:30:00",
      "inviteCode": null
    }
  ]
  ```

#### `POST /api/rooms/{roomId}/join`

사용자가 채팅방에 참여할 수 있도록 허용합니다.

- **경로 매개변수:**
  - `roomId`: `number` (채팅방 ID)
- **요청 본문 (선택 사항):**
  ```json
  {
    "nickname": "string (선택 사항)",
    "inviteCode": "string (선택 사항)"
  }
  ```
- **응답 (200 OK):** 빈 본문.
- **오류 (401 Unauthorized):** 사용자 인증되지 않음.
- **오류 (403 Forbidden):** 비공개 채팅방에 대한 초대 코드가 유효하지 않습니다.

### 태그 관리

#### `GET /api/tags/autocomplete`

자동 완성에 대한 태그 제안을 제공합니다.

- **쿼리 매개변수:**
  - `q`: `string` (태그 이름 접두사에 대한 쿼리 문자열)
- **응답 (200 OK):**
  ```json
  [
    {
      "name": "태그이름",
      "popularity": 10
    }
  ]
  ```

### 메시지 관리

#### `GET /api/rooms/{roomId}/messages`

채팅방의 이전 메시지를 검색합니다.

- **경로 매개변수:**
  - `roomId`: `number` (채팅방 ID)
- **쿼리 매개변수:**
  - `beforeId`: `number` (선택 사항, 페이징을 위한 커서. 이 ID보다 오래된 메시지가 반환됩니다.)
  - `limit`: `number` (선택 사항, 기본 30, 최대 50. 반환할 메시지 수.)
- **응답 (200 OK):**
  ```json
  [
    {
      "id": 101,
      "clientTempId": "uuid-string",
      "roomId": 1,
      "user": {
        "id": 1,
        "nickname": "앨리스",
        "avatarUrl": null
      },
      "type": "TEXT",
      "contentText": "안녕하세요 여러분!",
      "mediaUrl": null,
      "mediaThumbUrl": null,
      "mediaDurationSec": null,
      "createdAt": "2025-09-09T10:00:00"
    }
  ]
  ```

### 파일 업로드

#### `POST /api/upload/media`

미디어 파일(이미지, GIF, 비디오)을 업로드합니다.

- **요청 본문:** `file` 필드가 있는 `multipart/form-data`.
- **응답 (201 Created):**
  ```json
  {
    "type": "IMAGE",
    "url": "/uploads/image-uuid.jpg",
    "thumbUrl": "/uploads/thumb-uuid.jpg",
    "durationSec": null
  }
  ```
- **오류 (413 Payload Too Large):** 파일이 크기 제한을 초과합니다.
- **오류 (415 Unsupported Media Type):** 유효하지 않은 파일 형식입니다.

### 신고

#### `POST /api/messages/{messageId}/report`

부적절한 콘텐츠에 대해 특정 메시지를 신고합니다.

- **경로 매개변수:**
  - `messageId`: `number` (신고할 메시지 ID)
- **요청 본문:**
  ```json
  {
    "reason": "SPAM" | "ABUSE" | "NSFW" | "OTHER",
    "details": "string (선택 사항)"
  }
  ```
- **응답 (201 Created):** 빈 본문.
- **오류 (404 Not Found):** 메시지를 찾을 수 없습니다.
- **오류 (409 Conflict):** 이 사용자가 이미 이 메시지를 신고했습니다.