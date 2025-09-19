# TRD: Vibe Chat — 7일 MVP (Spring Boot 3.x + React 18 + TypeScript)

## 0. 문서 목적/범위
- 목적: AI 코딩 에이전트가 7일 내 MVP를 결정론적으로 구현할 수 있도록 기술 스펙, 작업 단위, 인터페이스, 검증 기준을 명확히 정의한다.
- 범위: 실시간 채팅(텍스트/이미지/GIF/≤10초 영상), 태그 기반 검색, 신고, 기본 인증(게스트/구글), Redis 캐싱/세션, MySQL 데이터 영속화, Docker 기반 로컬 데모.
- 비고: 동시 접속 100명, p95 메시지 지연 ≤ 1초, 반응형 UI, 다크모드(P2).

---

## 1. 기술 스택 및 버전
- Backend
  - Java 21, Spring Boot 3.3.x
  - Spring Web, Spring Data JPA, Spring Security, Spring WebSocket + STOMP(spring-messaging)
  - Spring Session Data Redis
  - MySQL 8.0, Flyway(스키마 마이그레이션)
  - Redis 7.x
  - ModelMapper(DTO 매핑), Jackson
  - Validation(jakarta.validation)
  - Apache Tika(파일 MIME 감지), Thumbnailator(이미지 리사이즈/썸네일)
  - net.bramp.ffmpeg(FFmpeg 래퍼; FFmpeg 바이너리 Docker 이미지에 포함)
  - Bucket4j(Spring Boot Starter, Redis 백엔드; 메시지 레이트 제한)
  - OWASP Java HTML Sanitizer(XSS/콘텐츠 정제)
  - springdoc-openapi-starter-ui(REST API 문서)
  - Lombok(개발 생산성)
- Frontend
  - React 18, TypeScript 5, Vite
  - Redux Toolkit(전역 상태), React Query(서버 상태), React Router
  - Tailwind CSS, Headless UI(모달/액션), Heroicons(아이콘)
  - Axios(HTTP), @stomp/stompjs + sockjs-client(STOMP over SockJS; 주의: socket.io 사용 금지)
  - react-hook-form + zod(폼/검증), react-toastify(토스트), date-fns(시간 포맷)
- Dev/Infra
  - Docker/Docker Compose
  - Nginx(리버스 프록시: /api, /ws 라우팅 + 정적 파일 서빙)
  - Postman 컬렉션, Swagger UI
  - JUnit5, Mockito(백엔드), Vitest/Jest + React Testing Library(프론트), WebSocket 통합 테스트(Spring)
  - GitHub Actions(선택, 로컬 우선)

검증 기준:
- 의존성 설치 후 애플리케이션 빌드/실행 성공.
- SockJS + STOMP를 통해 두 브라우저 간 메시지 송수신 p95 ≤ 1초.

---

## 2. 아키텍처 및 통신 흐름
- 3-Tier
  - Client(React) ↔ Nginx ↔ Backend(Spring Boot) ↔ MySQL/Redis
- 통신
  - REST API: Nginx → Backend(/api/**)
  - WebSocket(STOMP): Nginx → Backend(/ws)
  - 정적 파일/업로드: Nginx(프론트 빌드 파일, 업로드된 미디어)
- WebSocket 전략
  - 엔드포인트: /ws (SockJS 허용)
  - STOMP 브로커: SimpleBroker(in-memory) + 애플리케이션 목적지 프리픽스 /app
  - 하트비트: client↔server 10000/10000 ms
  - 구독 토픽: /topic/rooms/{roomId}/messages, /topic/rooms/{roomId}/typing, /topic/rooms/{roomId}/presence
  - 전송 목적지: /app/rooms/{roomId}/send, /app/rooms/{roomId}/typing
- 세션/인증
  - 비로그인: 게스트 닉네임(세션/쿠키) → Spring Session Redis
  - 소셜 로그인(구글): P1, OAuth2 Client
  - CSRF: REST CSRF 활성, WebSocket Handshake 기원(origin) 검증
- 캐시/상태
  - Redis: 세션, 온라인 존재(SET), 타이핑(SET with TTL), 태그/검색 캐시, 레이트 제한 버킷

검증 기준:
- /ws STOMP 연결 성공, 구독/송신 정상.
- CONNECT/DISCONNECT 시 presence 집계 반영.

---

## 3. 프로젝트 폴더 구조
- 루트
  - backend/
    - src/main/java/com/vibechat/...
      - config, security, websocket, controller, service, repository, domain(entity), dto, mapper, media, cache, util, scheduler
    - src/main/resources/application.yml
    - src/test/java/... (unit/integration/websocket)
    - Dockerfile
  - frontend/
    - src/
      - app(router/providers), pages, features(rooms/messages/user), components(ui/shared), store, services(api/ws), hooks, styles, assets
    - public/
    - vite.config.ts, tailwind.config.js, tsconfig.json
    - Dockerfile
  - infra/
    - docker-compose.yml
    - nginx/
      - nginx.conf
    - db/
      - init.sql (개발용), migration(Flyway)
    - scripts/
      - seed.sh, ffmpeg-check.sh
  - docs/
    - ERD.md, API.md, WEBSOCKET.md, REPORTING.md
  - README.md
  - .env, .env.example

검증 기준:
- docker-compose up으로 모든 서비스 기동, 프론트 접속/채팅 송수신 가능.

---

## 4. 데이터 모델(ERD 텍스트)
- users
  - id(BIGINT, PK, AI)
  - provider(VARCHAR20, GUEST|GOOGLE)
  - provider_id(VARCHAR128, nullable for GUEST)
  - nickname(VARCHAR32, unique within room via runtime check)
  - avatar_url(VARCHAR255, nullable)
  - created_at(DATETIME, idx)
- chat_rooms
  - id(BIGINT, PK, AI)
  - title(VARCHAR80, idx)
  - description(VARCHAR255, nullable)
  - is_private(TINYINT1, idx)
  - invite_code(CHAR36, unique, nullable for public)
  - created_by(BIGINT, FK→users.id, idx)
  - created_at(DATETIME, idx), updated_at(DATETIME)
- tags
  - id(BIGINT, PK, AI)
  - name(VARCHAR32, unique, idx(prefix))
  - popularity(INT, default 0, idx)
- room_tags
  - room_id(BIGINT, FK→chat_rooms.id)
  - tag_id(BIGINT, FK→tags.id)
  - PK(room_id, tag_id), idx(tag_id)
- messages
  - id(BIGINT, PK, AI)
  - room_id(BIGINT, FK→chat_rooms.id, idx)
  - user_id(BIGINT, FK→users.id, idx)
  - type(ENUM: TEXT, IMAGE, GIF, VIDEO, idx)
  - content_text(TEXT, nullable)
  - media_url(VARCHAR255, nullable)
  - media_thumb_url(VARCHAR255, nullable)
  - media_duration_sec(SMALLINT, nullable)
  - client_temp_id(VARCHAR36, nullable, idx)
  - is_deleted(TINYINT1, default 0)
  - created_at(DATETIME, idx DESC)
- message_reports
  - id(BIGINT, PK, AI)
  - message_id(BIGINT, FK→messages.id, idx)
  - reporter_user_id(BIGINT, FK→users.id, nullable)
  - reason(VARCHAR64)
  - details(VARCHAR255, nullable)
  - created_at(DATETIME)

인덱싱 전략:
- messages(room_id, created_at DESC) 커버링 인덱스
- room_tags(room_id, tag_id), room_tags(tag_id)
- tags(name prefix 인덱스), tags(popularity)
- chat_rooms(is_private, created_at), chat_rooms(title)

보관 정책(하우스키핑):
- 방별 최근 1,000개 메시지 유지. cron(스케줄러)로 방 단위 created_at DESC 기준 트림.
- 미디어 파일은 메시지 삭제 시 soft delete만(데모 범위).

검증 기준:
- Flyway 마이그레이션으로 스키마 생성 완료.
- 메시지 로드/페이지네이션 쿼리 30개 기준 ≤ 50ms(로컬).

---

## 5. Redis 키 설계
- Spring Session: spring:session:* (프레임워크 관리)
- Presence
  - presence:room:{roomId}:sessions = SET(STOMP_SESSION_ID)
  - presence:session:{stompSessionId} = HASH{ userId, roomId } (TTL 1h)
  - presence:room:{roomId}:count = INT 캐시(선택)
- Typing
  - typing:room:{roomId} = SET(userId) with TTL 3s(주기적 refresh)
- Cache
  - cache:tags:prefix:{q} = JSON[list of tags], TTL 5m
  - cache:rooms:search:{tagsHash} = JSON[list of rooms], TTL 1m
- Rate Limit(Bucket4j)
  - rl:msg:{userId}:{roomId}
- Misc
  - nickname:room:{roomId} = SET(nickname) with TTL session scope

검증 기준:
- 입장/퇴장 시 presence 세트 업데이트 및 브로드캐스트 반영.
- 타이핑 이벤트 수신 시 1초 내 반영.

---

## 6. API 사양(REST)
- 공통
  - Base URL: /api
  - Auth: 세션 쿠키(게스트), OAuth2(구글, P1)
  - CSRF: 쿠키/헤더 더블 서밋
  - 응답: application/json, RFC7807 에러 포맷 권장
  - 페이징: cursor 방식(beforeId 또는 beforeTimestamp), limit(기본 30, 최대 50)

- 엔드포인트
  - POST /api/users/guest
    - body: { nickname: string }
    - 201: { userId, nickname }
    - 409: 닉네임 중복(방 컨텍스트 없는 경우 프론트에서 방 입장 시 중복 확인 재수행)
  - POST /api/rooms
    - body: { title, description?, isPrivate, tags: string[] }
    - 201: { id, inviteCode?, ... }
  - GET /api/rooms/{roomId}
    - 200: { id, title, isPrivate, tags, participantsCount, lastMessageAt }
  - GET /api/rooms/search?tags=tag1,tag2
    - 200: [{ id, title, tags, isPrivate, participantsCount }]
  - GET /api/tags/autocomplete?q=pre
    - 200: [{ name, popularity }]
  - POST /api/rooms/{roomId}/join
    - body: { nickname?, inviteCode? }
    - 200: { joined: true }
    - 401/403: 초대코드 오류/권한 없음
  - GET /api/rooms/{roomId}/messages?beforeId=&limit=30
    - 200: [{ id, user, type, contentText, mediaUrl, mediaThumbUrl, createdAt }]
  - POST /api/upload/media
    - form-data: file
    - 201: { type, url, thumbUrl?, durationSec? }
    - 검증: 이미지/GIF/MP4만, 크기 제한(예: ≤ 20MB), 영상 ≤ 10초
  - POST /api/messages/{messageId}/report
    - body: { reason, details? }
    - 201: { reported: true }

검증 기준:
- Swagger UI에서 모든 P0 엔드포인트 호출 성공.
- 잘못된 입력 시 4xx, 메시지 명확.

---

## 7. WebSocket(STOMP) 이벤트 사양
- 연결
  - Endpoint: /ws (SockJS)
  - Subscribe:
    - /topic/rooms/{roomId}/messages
    - /topic/rooms/{roomId}/typing
    - /topic/rooms/{roomId}/presence
- 송신(App Destinations)
  - /app/rooms/{roomId}/send
    - payload: { clientTempId, type, contentText?, mediaUrl?, mediaThumbUrl?, durationSec? }
  - /app/rooms/{roomId}/typing
    - payload: { typing: true|false }
- 브로드캐스트 페이로드
  - MessageEvent:
    - { id, clientTempId?, roomId, user: { id, nickname, avatarUrl? }, type, contentText?, mediaUrl?, mediaThumbUrl?, durationSec?, createdAt }
  - TypingEvent:
    - { roomId, userId, nickname, typing: true|false, ts }
  - PresenceEvent:
    - { roomId, count }

ACK/전송 품질:
- 클라이언트는 clientTempId(UUID)를 포함해 전송.
- 서버는 영속화 후 같은 clientTempId를 포함해 MessageEvent 브로드캐스트 → 클라이언트는 pending→sent 전환.
- 실패 시 서버는 ERROR frame 또는 REST 재시도 안내.

검증 기준:
- 두 브라우저에서 텍스트/이미지 메시지 송수신, 보낸 쪽은 pending→sent 상태 전환.
- 타이핑/입장 수가 1초 내 반영.

---

## 8. 비즈니스 로직 규칙
- 닉네임 중복: 방 입장 시 roomId 스코프 내 중복 불가. 중복 시 서버가 제안(닉네임+숫자) 반환.
- 비공개 방: inviteCode 일치해야 join 성공.
- 메시지 검증: 길이 제한(텍스트 2,000자), 금지 문자/스크립트 제거(HTML Sanitizer).
- 메시지 보관: 방별 최신 1,000개만 유지(트림 작업은 비동기 스케줄러).
- 신고: 동일 메시지에 동일 사용자 중복 신고 방지.

검증 기준:
- 각 규칙 위반 시 명확한 오류 메시지/사유 제공.

---

## 9. 미디어 업로드/처리 정책
- 허용 형식
  - 이미지: image/jpeg, image/png, image/webp
  - GIF: image/gif
  - 비디오: video/mp4(H.264/AAC)
- 제한
  - 최대 크기: 기본 20MB(환경 변수로 조정)
  - 영상 길이: ≤ 10초(FFmpeg probe로 검증)
- 처리
  - 이미지: Thumbnailator(긴 변 기준 512px 썸네일, JPEG/WebP)
  - GIF: 원본 유지, 첫 프레임 썸네일 생성
  - 영상: FFmpeg로 1초 지점 썸네일 추출(poster), durationSec 저장(트랜스코딩 없음)
- 저장
  - 로컬 파일시스템(/var/www/uploads) + Docker 볼륨 마운트
  - 정적 서빙: Nginx /uploads 경로 매핑
- 보안
  - Apache Tika로 MIME 확인(확장자 신뢰 금지)
  - 파일명 랜덤 UUID + 확장자
  - 업로드 경로 디렉터리 트래버설 방지

검증 기준:
- 크기/길이/형식 위반 시 4xx 반환, 토스트로 사유 표시.
- 업로드 후 미리보기/썸네일 노출.

---

## 10. 보안/안전
- Spring Security
  - 세션 기반 인증(게스트), OAuth2 구글(P1)
  - REST CSRF 보호, WebSocket Origin 화이트리스트(로컬 데모 도메인)
- 입력 검증/정제
  - DTO Validation(@NotBlank, @Size 등), 메시지 본문 HTML Sanitizer
- Rate Limiting
  - 메시지 전송: 기본 20/min(버스트 10), 초과 시 429 + 남은 대기시간
- 파일 업로드
  - MIME 검사, 크기 제한, 임시 디렉터리 격리
- 헤더/정책
  - Nginx: Security headers(Referrer-Policy, X-Content-Type-Options, X-Frame-Options SAMEORIGIN, CSP는 데모 범위 내 기본)
- 로깅/감사
  - JSON 로그 + 요청 correlationId
  - 신고 이벤트 감사 로그

검증 기준:
- 레이트 제한 트리거 시 적절한 오류 및 재시도 대기 안내.
- XSS 페이로드 반영되지 않음.

---

## 11. 성능/확장 전략
- DB
  - 메시지 페이지네이션: 인덱스 기반 cursor(beforeId)
  - Connection pool: HikariCP, 최대 20(로컬)
- Redis
  - 태그/검색 캐시 TTL로 p50 응답 100~300ms 유지
- WebSocket
  - STOMP Heartbeat 10s, 재연결 지수 백오프(1s→2s→4s, 최대 10s)
  - 브로드캐스트 패스 최소화(컨트롤러→브로커)
- 프론트
  - 가상 스크롤(메시지 리스트), React.memo/useMemo 적용
  - 이미지 썸네일 우선 로드, 지연 로딩
- 목표
  - 100 동시 접속에서 서버 CPU ≤ 70%, p95 메시지 지연 ≤ 1s

검증 기준:
- 로컬 부하(가상 사용자) 100 연결에서 메시지 손실 1% 이하.

---

## 12. 프론트엔드 설계
- 라우팅
  - / : 홈(검색/방목록)
  - /rooms/:roomId : 채팅방
  - /create : 방 생성 모달/페이지
  - /error, /404
- 전역 상태(Redux slices)
  - user: { id, nickname, avatarUrl, auth: guest|google }
  - rooms: { list, searchQuery, tags, selectedRoom }
  - messages: byRoomId { items[], hasMore, loading, pendingByClientId }
- 서버 상태(React Query)
  - tags.autocomplete(q)
  - rooms.search(tags)
  - rooms.detail(roomId)
  - messages.fetch(roomId, cursor)
- WebSocket 클라이언트
  - singleton StompClient, 자동 재연결, 전송 큐(오프라인 시 보관)
  - 구독 관리: messages, typing, presence
- UI/UX
  - 반응형 브레이크포인트(sm, md, lg)
  - 다크모드(P2): prefers-color-scheme + 토글(LocalStorage)
  - 스켈레톤/로딩 상태(P1), 토스트 에러 표시
- 폼
  - react-hook-form + zod: 방 생성/닉네임/신고

검증 기준:
- 첫 방문→닉네임 설정→방 입장→첫 메시지 ≤ 60초.
- 메시지 전송 실패 시 재시도 버튼 노출, 성공 시 pending 제거.

---

## 13. 백엔드 모듈/인터페이스
- Controller
  - RoomController, TagController, MessageController(Read-only), UploadController, ReportController, AuthController
- WebSocket Controller
  - ChatWsController: /app/rooms/{id}/send, /typing
  - EventListener: CONNECT/DISCONNECT, SUBSCRIBE/UNSUBSCRIBE → presence 갱신
- Service
  - RoomService: 생성/조회/검색, 초대코드 검증
  - MessageService: 저장/조회/트림
  - TagService: 자동완성/연관 태그, 캐시
  - UploadService: 검증/저장/썸네일/FFmpeg
  - ReportService: 신고 저장/중복 검사
  - PresenceService: Redis 세트 관리/카운트 브로드캐스트
  - RateLimitService: Bucket4j 검사
- Repository(JPA)
  - UserRepository, RoomRepository, MessageRepository, TagRepository, RoomTagRepository, ReportRepository
- Mapper
  - ModelMapper 빈 설정, DTO↔Entity 변환
- Config
  - SecurityConfig, WebSocketConfig, RedisConfig, SwaggerConfig, ModelMapperConfig, ValidationConfig
- Scheduler
  - MessageTrimJob(방별 1,000개 유지), CacheWarmup(optional)

검증 기준:
- 각 Service에 단위 테스트 존재, 핵심 happy path 통과.

---

## 14. DTO/스키마(요약)
- REST
  - RoomCreateRequest: { title: string(≤80), description?: string(≤255), isPrivate: boolean, tags: string[1..5] }
  - RoomResponse: { id, title, isPrivate, tags[], participantsCount, lastMessageAt }
  - MessageResponse: { id, roomId, user:{id,nickname,avatarUrl?}, type, contentText?, mediaUrl?, mediaThumbUrl?, durationSec?, createdAt }
  - UploadResponse: { type, url, thumbUrl?, durationSec? }
  - ReportRequest: { reason: enum['spam','abuse','nsfw','other'], details? }
- WS
  - SendMessagePayload: { clientTempId: uuid, type: 'TEXT'|'IMAGE'|'GIF'|'VIDEO', contentText?, mediaUrl?, mediaThumbUrl?, durationSec? }
  - BroadcastMessage: MessageResponse & { clientTempId? }

검증 기준:
- 스키마 위반 시 400 + 필드별 에러 메시지.

---

## 15. 레이트 제한 규칙
- 메시지 전송: 20/분, burst 10
- 신고 제출: 5/분
- 태그 자동완성: 30/분(IP 기준)
- 응답: 429 + { retryAfterSeconds }

검증 기준:
- 강화 테스트로 임계 초과 시 429 반환.

---

## 16. 로깅/모니터링
- Logback JSON 패턴, 요청 ID(MDC) 주입
- 액추에이터: /actuator/health, metrics(선택)
- 주요 이벤트: 메시지 저장 실패, 업로드 실패, 신고 접수, 레이트 제한 트리거

검증 기준:
- 에러 발생 시 스택/컨텍스트 로그 확인 가능.

---

## 17. Docker/환경 구성
- docker-compose.yml
  - services: nginx, backend, frontend, mysql, redis
  - 볼륨: mysql-data, redis-data, uploads, frontend-dist
  - 네트워크: vibechat
- Nginx
  - / → 프론트 정적
  - /api → backend:8080
  - /ws → backend:8080 (Upgrade 헤더 유지)
  - /uploads → 로컬 볼륨 마운트
- Backend Dockerfile
  - 베이스: eclipse-temurin:21-jdk
  - FFmpeg 설치(apt-get), jar 실행
- Frontend Dockerfile
  - node:20 빌드 → nginx 또는 serve로 서빙(nginx 통합 시 dist만 마운트)
- 환경 변수(.env)
  - BACKEND:
    - DB_URL, DB_USER, DB_PASS
    - REDIS_HOST, REDIS_PORT
    - MAX_UPLOAD_MB=20, MAX_VIDEO_SEC=10
    - ALLOWED_ORIGINS
    - RATE_LIMITS(옵션)
  - FRONTEND:
    - VITE_API_BASE, VITE_WS_URL

검증 기준:
- docker-compose up 후 http://localhost 에서 데모 동작.
- /ws 업그레이드 정상.

---

## 18. 테스트 전략
- 백엔드
  - 단위: Service/Validator(비즈니스 규칙, 파일 검증)
  - 통합(MockMvc): 방 생성/검색/메시지 조회/신고/업로드
  - WebSocket: STOMP 클라이언트로 send/subscribe, presence/typing 반영
- 프론트
  - 단위: 컴포넌트 렌더/폼 검증/상태 리듀서
  - 통합: 메시지 전송 흐름(pending→sent), 자동 스크롤, 에러 토스트
- 시나리오(E2E 대체)
  - A: 닉네임→태그 검색→방 입장→텍스트 전송
  - B: 방 생성(비공개)→초대 코드 입장
  - C: 이미지/GIF/영상 업로드→미리보기→전송
  - D: 신고 제출 3스텝 완료

검증 기준:
- P0 플로우 테스트 전부 Green.

---

## 19. 작업 분해/의존성/우선순위
- Day 1
  - P0: 레포/모노레포 구성, Docker Compose, Flyway, 기본 엔티티/리포지토리, 보일러플레이트(Security/Session/Swagger)
  - P0: ERD 구현/인덱스, 더미 데이터 Seed
  - 검증: DB 연결, /actuator/health OK
- Day 2
  - P0: 방 CRUD(생성/조회), 태그 검색/자동완성, RoomService
  - P0: DTO/ModelMapper 설정
  - P1: Query 튜닝 및 캐시(태그 프리픽스)
  - P2: Postman 컬렉션
  - 검증: 검색→상세 플로우 완료
- Day 3
  - P0: WebSocket/STOMP 설정, 보안/오리진, SimpleBroker
  - P0: ChatWsController(send/subscribe), 메시지 저장/브로드캐스트, clientTempId 에코
  - P0: presence(입장/퇴장) 및 Redis 세트, typing 이벤트
  - P1: WS 에러 핸들링/재시도 정책 노출
  - 검증: 두 브라우저 간 텍스트 p95 ≤ 1초
- Day 4
  - P0: 프론트 레이아웃/라우팅/방목록/검색/반응형
  - P0: 방 생성 모달, 닉네임 입력, Redux slices, Axios 서비스
  - 검증: 게스트 닉네임→방 생성→입장→전송 단일 여정 동작
- Day 5
  - P0: 채팅 UI(리스트/입력창), 자동 스크롤, WS 연결
  - P0: 메시지 페이징(과거 로딩), 최초 30개 로딩
  - P1: 타이핑 표시, 온라인 사용자 수
  - P1: 전송 실패 재시도
  - 검증: 타이핑/온라인 1초 내 반영
- Day 6
  - P1: 업로드 API(검증/저장), 이미지 썸네일/리사이즈
  - P1: 프론트 업로드 UI, 미리보기
  - P2: 드래그앤드롭, GIF/영상 포스터(FFmpeg)
  - 검증: 제한 위반 차단/안내, 업로드 성공률 ≥95%
- Day 7
  - P2: 다크모드, 스켈레톤, 404/에러 화면
  - P1: 성능 최적화(메모/배치/가상 스크롤)
  - P0: README, Swagger 정리, 시연 스크립트
  - 검증: KPI(p95 ≤ 1초), 문서 100%

의존성 규칙:
- WebSocket 구현 전 방/태그 도메인 완성 필요.
- 업로드 API 전 Nginx 정적 라우트/볼륨 구성 필요.
- 프론트 WS 전 Backend /ws 가용 필요.

---

## 20. 검증 체크리스트(완료 기준)
- 기능(P0)
  - 게스트 닉네임 입장, 방 생성/참여(공개/비공개), 태그 검색, 텍스트/이미지/GIF/≤10초 영상 전송, 실시간 송수신, 신고, 반응형, 메시지 보관(1,000), 스크롤/과거 로딩
- 비기능
  - 100 동시 접속에서 메시지 성공률 ≥ 99%, p95 ≤ 1s
  - 초기 진입→첫 메시지 ≤ 60s
  - 로깅/문서화 완료(Swagger + README)
- 보안
  - XSS/CSRF 대응, 파일 검증, 레이트 제한 동작
- 배포(로컬)
  - docker-compose up → 단일 커맨드 데모 가능

---

## 21. 에러 코드/상태(예시)
- 400: VALIDATION_ERROR({ field, message })
- 401: UNAUTHORIZED
- 403: FORBIDDEN / INVALID_INVITE_CODE
- 404: NOT_FOUND
- 409: NICKNAME_CONFLICT
- 413: PAYLOAD_TOO_LARGE
- 415: UNSUPPORTED_MEDIA_TYPE
- 429: RATE_LIMIT_EXCEEDED({ retryAfterSeconds })
- 500: INTERNAL_ERROR

검증 기준:
- 각 시나리오에서 적절한 코드와 메시지 확인.

---

## 22. 성과 지표/KPI 수집(데모 범위)
- 측정 포인트
  - WS 전송→브로드캐스트→수신까지 지연(클라이언트 타임스탬프)
  - 전송 실패/재시도 횟수
  - 업로드 성공/실패율
- 수단
  - 프론트 로깅(콘솔/개발자용 오버레이), 간단한 메트릭 집계(옵션)

검증 기준:
- 데모 세션에서 KPI 값 캡처 가능.

---

## 23. 위험/완화
- 시간 부족 → P1/P2 축소, 영상 썸네일 생략 가능(대신 이미지/GIF 우선)
- 브라우저 호환 → 최신 크로뮴/사파리/파이어폭스 테스트 우선, 모바일 크로뮴 필수
- 성능 튜닝 → 메시지 렌더 가상화, 이미지 썸네일 강제

---

## 24. 라이브러리 대체 불가/호환성 주의
- STOMP 기준: socket.io-client는 Spring STOMP와 비호환. 반드시 sockjs-client + @stomp/stompjs 사용.
- FFmpeg: Docker 이미지에 설치하지 않으면 영상 길이 검증/포스터 생략.

---

## 25. 환경 변수 상세
- Backend
  - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/vibechat?useSSL=false&serverTimezone=UTC
  - SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD
  - SPRING_REDIS_HOST=redis, SPRING_REDIS_PORT=6379
  - SECURITY_ALLOWED_ORIGINS=http://localhost:5173
  - MEDIA_MAX_UPLOAD_MB=20
  - MEDIA_MAX_VIDEO_SECONDS=10
  - MEDIA_UPLOAD_DIR=/var/www/uploads
- Frontend
  - VITE_API_BASE=/api
  - VITE_WS_URL=ws://localhost/ws

검증 기준:
- .env.example 작성, .env 로딩 후 애플리케이션 정상 기동.

---

## 26. 문서화 산출물
- Swagger UI(/swagger-ui.html): REST 문서
- Postman 컬렉션: 주요 REST/업로드/시나리오
- README: 아키텍처, 실행법, 스크린샷/데모 플로우
- WEBSOCKET.md: 구독/송신 예제 페이로드, 재연결 전략
- ERD.md: 테이블 정의/인덱스/관계 설명
- REPORTING.md: 신고 사유/흐름/관리 방법

검증 기준:
- Day 7 문서 100% 충족, 실행 오류 없이 데모 가능.