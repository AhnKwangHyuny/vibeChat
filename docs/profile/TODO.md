# VibeChat 프로필 서비스 구축 TODO 리스트

> 이 문서는 VibeChat의 프로필 서비스 구축을 위한 기술적 요구사항과 단계별 실행 계획을 정의합니다. MSA(Microservice Architecture) 원칙에 따라 확장성과 유지보수성을 고려하여 설계되었습니다.

## 1. 목표

- 기존 Mock Data로 구현된 프로필 페이지를 실제 API와 연동하여 동적으로 작동하도록 구현합니다.
- 게스트 사용자(Guest)와 정식 사용자(Member) 모두에게 닉네임 수정, 프로필 사진 변경, 활동 통계 조회 등 일관된 프로필 경험을 제공합니다.
- Amazon S3를 활용하여 프로필 이미지를 안정적이고 효율적으로 처리합니다.

## 2. 기술 스택 및 아키텍처 결정

- **API 서버**: `api-server` (Spring Boot)에 사용자 관련 API를 통합하여 관리합니다. 별도의 `user-service`를 고려할 수 있으나, 초기 MVP 단계에서는 `api-server` 내에 `user` 도메인을 두어 관리 복잡성을 줄입니다.
- **이미지 저장소**: **Amazon S3**를 사용하여 프로필 이미지를 저장 및 서빙합니다.
- **이미지 업로드 방식**: **Presigned URL** 방식을 채택하여 클라이언트가 서버를 거치지 않고 직접 S3에 업로드하도록 구현합니다. 이는 서버의 부하를 줄이고 보안을 강화하는 실무 표준 방식입니다.
- **데이터베이스**: MySQL을 사용하여 사용자 정보를 관리합니다.
- **캐싱**: Redis를 활용하여 자주 조회되는 사용자 프로필 정보를 캐싱하여 API 응답 속도를 향상시킵니다.

## 3. 프로필 기능 요구사항 및 API 설계

### 3.1. 공통: 사용자 프로필 조회

- **기능**: 특정 사용자의 프로필 정보를 조회합니다.
- **API Endpoint**: `GET /api/v1/users/{userId}/profile`
- **Request**:
  - Path Variable: `userId` (String, 사용자 ID)
- **Response (Success: 200 OK)**:
  ```json
  {
    "userId": "string",
    "nickname": "string",
    "avatarUrl": "string (S3 URL)",
    "bio": "string",
    "joinedAt": "datetime",
    "status": "ONLINE | AWAY | OFFLINE",
    "userType": "GUEST | MEMBER",
    "stats": {
      "totalMessages": "long",
      "roomsJoined": "integer"
    }
  }
  ```
- **구현 노트**:
  - `stats`의 `friendsCount`는 친구 기능 구현 후 추가합니다.
  - `badges`는 뱃지 시스템 구현 후 추가합니다.
  - Redis에 프로필 정보를 캐싱하여 DB 부하를 줄입니다. (Cache Key: `user:{userId}:profile`)

### 3.2. 공통: 닉네임 및 자기소개 수정

- **기능**: 사용자의 닉네임과 자기소개를 수정합니다.
- **API Endpoint**: `PATCH /api/v1/users/{userId}/profile`
- **Request**:
  - Path Variable: `userId`
  - Body:
    ```json
    {
      "nickname": "string",
      "bio": "string"
    }
    ```
- **Response (Success: 200 OK)**: 수정된 프로필 정보 반환 (3.1과 동일)
- **구현 노트**:
  - 닉네임 중복 검사 로직이 필요합니다.
  - 요청자와 `userId`가 일치하는지 권한 검사를 수행해야 합니다.
  - 프로필 정보 수정 시 Redis 캐시를 갱신(또는 삭제)해야 합니다.

### 3.3. 공통: 프로필 이미지 업로드 (S3 Presigned URL)

#### **Step 1: Presigned URL 생성 요청**

- **기능**: 클라이언트가 S3에 직접 업로드할 수 있는 임시 URL을 발급받습니다.
- **API Endpoint**: `POST /api/v1/users/{userId}/profile-image/presigned-url`
- **Request**:
  - Path Variable: `userId`
  - Body:
    ```json
    {
      "fileName": "string (e.g., profile.jpg)",
      "contentType": "string (e.g., image/jpeg)"
    }
    ```
- **Response (Success: 200 OK)**:
  ```json
  {
    "presignedUrl": "string (S3 Presigned URL)",
    "imageUrl": "string (업로드 완료 후 최종 접근 URL)"
  }
  ```
- **구현 노트**:
  - S3 SDK를 사용하여 Presigned URL을 생성합니다.
  - 업로드될 파일 경로(Key)는 `profiles/{userId}/{uuid}_{fileName}` 형식으로 지정하여 중복을 방지합니다.
  - URL 만료 시간은 5분 내외로 짧게 설정합니다.

#### **Step 2: S3 업로드 완료 후 서버에 알림**

- **기능**: 클라이언트가 S3 업로드 완료 후, 해당 이미지 URL을 서버에 저장하도록 요청합니다.
- **API Endpoint**: `PUT /api/v1/users/{userId}/profile-image`
- **Request**:
  - Path Variable: `userId`
  - Body:
    ```json
    {
      "imageUrl": "string (Step 1에서 받은 최종 URL)"
    }
    ```
- **Response (Success: 200 OK)**:
  ```json
  {
    "message": "Profile image updated successfully."
  }
  ```
- **구현 노트**:
  - DB의 `user` 테이블에 `avatarUrl` 컬럼을 업데이트합니다.
  - 프로필 정보 수정이므로 Redis 캐시를 갱신합니다.

### 3.4. 게스트(Guest) 사용자 처리 방안

- **식별**: 게스트 사용자는 프론트엔드에서 생성된 UUID를 `localStorage`에 저장하여 식별합니다. API 요청 시 이 ID를 사용합니다.
- **데이터 저장**:
  - 게스트 사용자가 최초 프로필 관련 작업을 수행할 때, `user` 테이블에 `userType='GUEST'`로 데이터를 생성합니다.
  - 이 데이터는 정식 가입 시 `userType='MEMBER'`로 전환되고, 소셜 로그인 정보와 통합될 수 있습니다.
  - 특정 기간(예: 30일) 동안 활동이 없는 게스트 데이터는 스케줄링 작업으로 삭제하여 DB를 정리합니다.

## 4. 단계별 개발 로드맵 (TODO)

### **Phase 1: Backend (API 서버) 구현**

-   [ ] **DB Schema 수정**: `user` 테이블에 `bio` (TEXT), `status` (VARCHAR), `userType` (VARCHAR), `avatarUrl` (VARCHAR) 컬럼 추가.
-   [ ] **DTO 정의**: `ProfileResponseDto`, `ProfileUpdateRequestDto`, `PresignedUrlRequestDto` 등 API 명세에 맞는 DTO 클래스 작성.
-   [ ] **Controller 구현**:
    -   [ ] `GET /api/v1/users/{userId}/profile`
    -   [ ] `PATCH /api/v1/users/{userId}/profile`
    -   [ ] `POST /api/v1/users/{userId}/profile-image/presigned-url`
    -   [ ] `PUT /api/v1/users/{userId}/profile-image`
-   [ ] **Service 로직 구현**:
    -   [ ] 사용자 프로필 조회, 수정 로직 구현.
    -   [ ] S3 Presigned URL 생성 로직 구현 (AWS SDK 연동).
    -   [ ] 게스트 사용자 생성 및 관리 로직 구현.
-   [ ] **Repository 구현**: `UserRepository`에 필요한 쿼리 메소드 추가.
-   [ ] **보안 설정**: 프로필 수정/업로드 API에 대해 본인만 접근 가능하도록 Spring Security 설정.
-   [ ] **캐싱 적용**: 프로필 조회 API에 Redis 캐싱 적용 (`@Cacheable`). 프로필 수정 시 캐시 무효화 (`@CacheEvict`).
-   [ ] **단위/통합 테스트**: 각 API 및 서비스 로직에 대한 테스트 코드 작성.

### **Phase 2: Frontend (React) 구현**

-   [ ] **API 서비스 연동**: `services/api`에 프로필 관련 API 호출 함수 추가.
-   [ ] **React Query 적용**:
    -   [ ] `useQuery`를 사용하여 프로필 정보를 비동기적으로 조회.
    -   [ ] `useMutation`을 사용하여 닉네임/자기소개 수정, 프로필 이미지 변경 기능 구현.
-   [ ] **`Profile.tsx` 페이지 수정**:
    -   [ ] Mock Data를 제거하고 React Query로 받아온 실제 데이터와 연동.
    -   [ ] 로딩 및 에러 상태에 대한 UI 처리 (스켈레톤 UI 활용).
-   [ ] **`ProfileCard.tsx` 컴포넌트 수정**:
    -   [ ] 닉네임, 자기소개 수정 UI와 `useMutation` 연동.
    -   [ ] 프로필 사진 클릭 시 파일 업로드 로직 구현.
        -   [ ] 파일 선택 -> Presigned URL 요청 -> S3 업로드 -> 서버에 완료 알림 순서로 진행.
        -   [ ] 업로드 진행 상태를 표시하는 UI 추가 (e.g., `Progress` 컴포넌트).
-   [ ] **상태 관리**: 사용자 상태(Online, Away 등) 변경 기능 구현 및 API 연동.
-   [ ] **게스트 로직**: `localStorage`에서 게스트 ID를 관리하고, API 요청 시 헤더나 요청 본문에 포함하여 전송.

### **Phase 3: 인프라 및 운영**

-   [ ] **S3 버킷 생성**: 프로필 이미지 저장을 위한 S3 버킷 생성 및 정책 설정 (Public Read 허용).
-   [ ] **IAM 설정**: `api-server`가 S3에 접근할 수 있는 권한을 가진 IAM 역할(Role) 또는 사용자(User) 생성.
-   [ ] **Spring Boot 설정**: `application.yml`에 AWS 자격증명 및 S3 버킷 정보 설정. (보안을 위해 환경변수 또는 AWS Secrets Manager 사용 권장)
-   [ ] **스케줄러 구현**: 장기 미사용 게스트 계정을 주기적으로 삭제하는 Spring Scheduler (`@Scheduled`) 구현.

---
> 이 로드맵을 따라 단계적으로 개발을 진행하면, 안정적이고 확장 가능한 프로필 서비스를 성공적으로 구축할 수 있을 것입니다. 각 단계별로 진행 상황을 공유하며 함께 리뷰해 나갑시다.
