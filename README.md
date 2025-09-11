# Vibe Chat

관심사 기반 실시간 채팅 플랫폼 "Vibe Chat"의 MVP 프로젝트입니다.

## 프로젝트 개요

같은 관심사를 가진 사람들이 "지금" 연결되어 대화를 시작할 수 있는 가장 간편한 실시간 채팅 경험을 제공합니다. 태그 기반 검색, 공개/비공개 방, 실시간 텍스트 및 미디어(이미지, GIF, 짧은 영상) 공유 등의 기능을 갖춘 웹 반응형 서비스입니다.

자세한 내용은 `prd.md`와 `trd.md` 문서를 참고하세요.

## 기술 스택

- **Backend:** Spring Boot 3.3 (Java 21), WebSocket, MySQL, Redis
- **Frontend:** React 18, TypeScript, Vite, Redux Toolkit, Tailwind CSS
- **Infrastructure:** Docker, Docker Compose, Nginx

## 디렉터리 구조

```
/
├── backend/         # Spring Boot 백엔드 애플리케이션
├── frontend/        # React 프론트엔드 애플리케이션
├── infra/           # Docker, Nginx 등 인프라 설정
├── docs/            # 프로젝트 관련 문서
├── README.md        # 프로젝트 개요 및 안내
└── ...
```

## 빠른 시작 (Quick Start)

이 프로젝트는 Docker Compose를 통해 모든 서비스(백엔드, 프론트엔드, DB, Redis 등)를 한 번에 실행할 수 있도록 구성되어 있습니다.

**요구사항:**
- Docker
- Docker Compose

**실행 방법:**

1.  **환경 변수 파일 생성**

    `.env.example` 파일을 복사하여 `.env` 파일을 생성하고, 필요에 따라 내부 변수 값을 수정합니다.

    ```bash
    cp .env.example .env
    ```

2.  **Docker Compose 실행**

    프로젝트 루트 디렉터리에서 아래 명령어를 실행합니다.

    ```bash
    docker-compose up --build
    ```

3.  **애플리케이션 접속**

    웹 브라우저에서 `http://localhost`로 접속합니다.
