# todos.md: 바이브 챗 프로젝트 할 일 목록 상태

이 문서는 `todo.yaml`에 정의된 바이브 챗 프로젝트의 모든 작업 및 하위 작업의 현재 완료 상태를 보여줍니다.

- ✓: 완료된 작업
- x: 미완료 작업

## 프로젝트 개요

✓ 실시간 채팅(텍스트/이미지/GIF/≤10초 영상), 태그 기반 검색, 신고, 기본 인증(게스트/구글[P1]), Redis 캐싱/세션, MySQL 영속화, Docker 기반 로컬 데모를 7일 내 구현하기 위한 결정론적 작업 계획 및 검증 기준 정의.

## 작업 목록

### 1. setup-monorepo (모노레포 구조 및 기본 환경 설정) - P0
✓ 프로젝트 루트/백엔드/프론트엔드/인프라 디렉터리 생성, 공통 .env.example 배치, README 스켈레톤 추가. Git ignore, 라이선스/에디터 설정 포함. Docker 기반 로컬 실행을 위한 기본 전제 구성.
  - ✓ 폴더 구조 생성
  - ✓ .env.example 작성
  - ✓ README 스켈레톤

### 2. infra-docker-compose (Docker Compose 서비스 구성) - P0
✓ nginx, backend, frontend, mysql, redis, 볼륨(mysql-data, redis-data, uploads, frontend-dist) 및 네트워크(vibechat) 정의. 업로드 경로와 정적 파일 서빙 볼륨 마운트.
  - ✓ 서비스 정의
  - ✓ 볼륨/네트워크 구성

### 3. backend-build-init (백엔드 빌드/의존성 초기화 및 보일러플레이트) - P0
✓ Spring Boot 3.3.x(Java 21) 프로젝트 초기화, 의존성 추가(Web, Security, JPA, WebSocket, Validation, Redis, Spring Session, flyway, modelmapper, jackson, tika, thumbnailator, bucket4j-redis, owasp sanitizer, springdoc-openapi, lombok). Actuator 활성. 기본 패키지/구성 클래스 생성.
  - ✓ 의존성 선언
  - ✓ Actuator 활성화
  - ✓ JSON 로깅/MDC

### 4. data-flyway-er (ERD 마이그레이션 및 인덱스 구현(Flyway)) - P0
✓ TRD 4항 ERD와 인덱싱 전략을 Flyway 스크립트로 구현. 개발용 초기 데이터 시드 스크립트 제공.
  - ✓ 스키마 정의
  - ✓ 마이그레이션 검증

### 5. backend-entity-repo (JPA 엔티티/리포지토리 정의) - P0
✓ ERD에 따른 엔티티 및 리포지토리 인터페이스 구현. 생성/조회 최소 연산 보장.
  - ✓ 엔티티 매핑
  - ✓ 리포지토리 메서드

### 6. backend-auth-guest (게스트 사용자 생성 API) - P0
✓ 세션 기반 게스트 닉네임 등록. 중복 검사는 방 입장 시 재확인. 세션 쿠키로 인증 유지.
  - ✓ DTO/검증
  - ✓ 세션 발급/저장

### 7. backend-room-tag-api (방 생성/조회/검색 및 태그 자동완성 API) - P0
✓ RoomService/TagService 구현. 방 생성, 상세 조회, 태그 기반 검색, 자동완성 캐시(P1).
  - ✓ 방 생성 DTO/검증
  - ✓ 태그 기반 검색
  - ✓ 태그 자동완성

### 8. backend-join-room (방 참가 API(공개/비공개)) - P0
✓ 닉네임(옵션)과 초대 코드 검증. 룸 스코프 닉네임 중복 방지. 제안 닉네임 반환 로직 포함.
  - ✓ 닉네임 중복 체크
  - ✓ 초대 코드 검증

### 9. backend-ws-config (WebSocket/STOMP 설정 및 보안) - P0
✓ /ws 엔드포인트, SockJS 허용, SimpleBroker, /app 프리픽스, 하트비트 10000/10000 설정. 오리진 화이트리스트 검증.
  - ✓ 엔드포인트/브로커 구성
  - ✓ 오리진/CSRF 정책

### 10. backend-presence-typing (Presence/Typing Redis 연동 및 브로드캐스트) - P0
✓ Redis 키 설계에 따라 입장/퇴장 시 presence 세트 갱신, 타이핑 이벤트 SET+TTL 처리. /topic/rooms/{roomId}/presence 및 /typing 브로드캐스트.
  - ✓ Presence 세트 관리
  - ✓ Typing 세트 관리

### 11. backend-message-core (메시지 저장/브로드캐스트 및 레이트 제한) - P0
✓ /app/rooms/{roomId}/send 처리. 메시지 DTO 검증, XSS 정제, Bucket4j로 20/min(버스트 10) 레이트 제한, 저장 후 clientTempId 포함 브로드캐스트. 메시지 조회 API(읽기) 포함.
  - ✓ 메시지 검증/정제
  - ✓ 레이트 제한
  - ✓ 브로드캐스트/ACK
  - ✓ 메시지 조회 API

### 12. backend-report-api (신고 API 구현) - P0
✓ POST /api/messages/{messageId}/report 엔드포인트. 동일 사용자 동일 메시지 중복 신고 방지. 감사 로그 기록.
  - ✓ DTO/검증
  - ✓ 감사 로깅

### 13. backend-upload-api (미디어 업로드 API(검증/저장/썸네일/FFmpeg)) - P1
✓ POST /api/upload/media 구현. Apache Tika MIME 검증, 크기 제한, 이미지 썸네일(Thumbnailator), GIF 첫 프레임 썸네일, MP4 길이 검증(≤10s) 및 1초 포스터 추출. 저장 경로 UUID 파일명, 디렉터리 트래버설 방지.
  - ✓ MIME/크기/길이 검증
  - ✓ 파일 저장/경로 안전화
  - ✓ 썸네일/포스터

### 14. backend-scheduler-trim (메시지 보관 트림 스케줄러) - P0
✓ 방별 최근 1,000개 메시지만 유지하도록 created_at DESC 기준 트림. 비동기 스케줄러 구현.
  - ✓ 트림 전략

### 15. backend-tests (백엔드 단위/통합/WS 테스트) - P0
✓ Service 단위 테스트, REST 통합(MockMvc), WebSocket 통합(STOMP 클라이언트) 작성. 레이트 제한/에러 경로 포함.
  - ✓ Service 단위 테스트
  - ✓ REST 통합 테스트
  - ✓ WS 통합 테스트

### 16. frontend-setup (프론트엔드 초기 설정(Vite/TS/Tailwind/Router/Redux/Query)) - P0
✓ React 18 + TS + Vite 초기화. Tailwind/Headless UI/Heroicons. Redux Toolkit, React Query, React Router 설정. 기본 레이아웃.
  - ✓ 스토어/슬라이스 골격
  - ✓ Axios 인스턴스

### 17. frontend-rooms-search (홈/방 목록/검색 UI + API 연동) - P0
✓ 홈 화면에서 태그 자동완성/검색, 방 리스트/상세 진입. React Query 훅 작성.
  - ✓ 검색 폼/검증
  - ✓ 리스트/페이지네이션

### 18. frontend-auth-guest-nickname (게스트 닉네임 설정 UI) - P0
✓ 최초 방문 시 닉네임 입력/검증, 세션 발급 API 연동. 로컬 스토리지에 표시용 캐싱.
  - ✓ 입력 검증

### 19. frontend-room-create-join (방 생성/참여 UI) - P0
✓ 방 생성 폼(비공개/태그), 초대코드/닉네임 기반 입장 흐름. 실패 시 원인 표시.
  - ✓ 방 생성 폼
  - ✓ 방 참가/초대코드

### 20. frontend-ws-client (STOMP 클라이언트 및 구독 관리) - P0
✓ "@stomp/stompjs + sockjs-client로 싱글턴 클라이언트 구성, 자동 재연결(지수 백오프), 전송 큐, 하트비트, 구독 관리(messages/typing/presence)."
  - ✓ 재연결/백오프
  - ✓ 토픽 구독/해제

### 21. frontend-chat-ui (채팅 UI(리스트/입력/전송) 및 페이징) - P0
✓ 메시지 리스트 가상 스크롤, 최초 30개 로딩, 과거 로딩(beforeId), 입력창/전송, pending→sent 관리(clientTempId 기준). 실패 재시도(P1).
  - ✓ 가상 스크롤/자동 스크롤
  - ✓ 전송/ACK 처리

### 22. frontend-presence-typing (타이핑/온라인 수 표시) - P1
✓ TypingEvent/PersistenceEvent 구독 후 UI 반영(1초 내).
  - ✓ Presence 표시
  - ✓ 타이핑 표시

### 23. frontend-upload-ui (업로드 UI/미리보기/전송) - P1
✓ 이미지/GIF/비디오 파일 선택, 업로드 API 연동, 미리보기, 메시지 전송에 mediaUrl/썸네일 사용. 드래그앤드롭(P2).
  - ✓ 클라이언트 검증
  - ✓ 업로드→메시지 전송

### 24. frontend-error-handling (에러/레이트 제한/재시도 UX) - P1
✓ RFC7807 에러 메시지 표준화 토스트, 메시지 전송 실패 재시도 버튼, 429 시 재시도 대기 안내.
  - ✓ 429 처리

### 25. infra-nginx-config (Nginx 리버스 프록시/정적 서빙) - P0
✓ / → 프론트 정적, /api → backend:8080, /ws → backend:8080(WebSocket 업그레이드), /uploads → 업로드 볼륨 매핑. 보안 헤더 적용.
  - ✓ WebSocket 업그레이드
  - ✓ 보안 헤더

### 26. docs-and-scripts (문서/스크립트 완성(README/Swagger/WEBSOCKET/ERD/REPORTING)) - P0
✓ Swagger 정리, Postman(P2) 옵션, README 실행법/시연 스크립트, WS 프로토콜 문서, ERD 설명, 신고 흐름 문서.
  - ✓ Swagger 그룹/태그/예시
  - ✓ 시연 스크립트

### 27. fe-darkmode-skeleton-404 (다크모드/스켈레톤/에러 화면) - P2
✓ prefers-color-scheme + 토글(LocalStorage), 리스트 스켈레톤, 404/에러 페이지.
  - ✓ 다크모드 토글
  - ✓ 스켈레톤 컴포넌트

### 28. perf-optimizations (성능 최적화(가상 스크롤/메모/백엔드 튜닝)) - P1
✓ 메시지 렌더 가상화, React.memo/useMemo, 백엔드 커넥션 풀/Hikari 설정, Redis 캐시 태그/검색, WS 브로드캐스트 경로 최소화.
  - ✓ 리스트 가상화
  - ✓ 태그/검색 캐시 튜닝

### 29. qa-scenario-tests (시나리오(E2E 대체) 점검) - P0
✓ "A: 닉네임→태그 검색→입장→텍스트 전송, B: 비공개 방 생성→초대 코드 입장, C: 업로드→미리보기→전송, D: 신고 3스텝. 수동/스크립트화 점검."
  - ✓ 시나리오 실행

## 전체 완료율

**100%**
