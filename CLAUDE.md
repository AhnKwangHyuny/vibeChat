# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Vibe Chat (바이브 챗) is a real-time, interest-based chat platform MVP built in 7 days. Users can quickly find and join chat rooms by tags, send text/media messages, and interact in real-time without registration (guest mode + optional Google OAuth).

**Tech Stack:**
- Backend: Spring Boot 3.3 (Java 21), WebSocket/STOMP, MySQL 8.0, Redis 7.x
- Frontend: React 18, TypeScript, Vite, Redux Toolkit, Tailwind CSS
- Infrastructure: Docker Compose, Nginx, FFmpeg (for video processing)

## Development Commands

### Frontend (React/TypeScript)
```bash
cd frontend
npm run dev          # Start development server
npm run build        # Build for production (runs TypeScript compiler + Vite build)
npm run lint         # Run ESLint
npm run preview      # Preview production build
```

### Backend (Spring Boot/Maven)
```bash
cd backend
mvn spring-boot:run  # Start development server
mvn clean compile    # Compile Java sources
mvn test            # Run tests
mvn clean package   # Build JAR file
```

### Full Application (Docker Compose)
```bash
docker-compose up --build    # Build and run all services
docker-compose down          # Stop all services
```

The application runs at `http://localhost` when using Docker Compose.

## Architecture

### Directory Structure
- `frontend/` - React application with Vite
- `backend/` - Spring Boot application with Maven  
- `infra/` - Docker configurations, Nginx setup, database scripts
- `docs/` - API documentation, ERD, WebSocket specs, demo checklist

### Frontend Architecture
- **State Management**: Redux Toolkit with feature-based slices (user, messages, presence, typing)
- **API Layer**: Axios with React Query for server state, session-based auth
- **Real-time**: STOMP client with auto-reconnect, exponential backoff, offline queue
- **UI**: Tailwind CSS + Headless UI, responsive design, dark mode support
- **Forms**: react-hook-form + Zod validation

### Backend Architecture  
- **REST API**: Controllers under `/api/*` for CRUD operations (rooms, messages, upload, reports)
- **WebSocket**: STOMP over `/ws` for real-time messaging, presence, typing indicators
- **Database**: MySQL with Flyway migrations, message retention (1,000 per room)
- **Caching**: Redis for sessions, presence sets, typing indicators, search cache
- **Security**: Spring Security, CSRF protection, rate limiting (Bucket4j), content sanitization
- **Media**: File upload with validation, thumbnail generation, FFmpeg video processing

### Key Features & Business Rules
- **Guest Authentication**: Nickname-based sessions, room-scoped uniqueness
- **Room Types**: Public (searchable) and private (invite code required)
- **Media Support**: Images, GIFs, videos ≤10 seconds with thumbnails
- **Rate Limiting**: 20 messages/minute per user per room (burst: 10)
- **Performance**: p95 message latency ≤1s, supports 100 concurrent users

## Development Guidelines

### Environment Setup
- Copy `.env.example` to `.env` and configure variables
- Database schema auto-created via Flyway on startup
- Redis required for sessions and real-time state management

### WebSocket Protocol
- **Endpoint**: `/ws` with SockJS fallback
- **Subscribe**: `/topic/rooms/{roomId}/messages|typing|presence`  
- **Send**: `/app/rooms/{roomId}/send` with clientTempId for ACK
- **Heartbeat**: 10s intervals, auto-reconnect with exponential backoff

### File Upload & Storage
- **Path**: `/var/www/uploads/` (Docker volume mounted)
- **Validation**: Apache Tika for MIME detection, size limits, FFmpeg for video duration
- **Processing**: Thumbnailator for image resize, FFmpeg for video posters
- **Security**: UUID filenames, directory traversal protection

### Redis Key Patterns
```
presence:room:{roomId}:sessions     # Active session IDs (SET)
presence:session:{sessionId}        # User/room mapping (HASH, TTL 1h)  
typing:room:{roomId}               # Currently typing users (SET, TTL 3s)
cache:tags:prefix:{query}          # Tag autocomplete cache (TTL 5m)
cache:rooms:search:{tagsHash}      # Search results cache (TTL 1m)
rl:msg:{userId}:{roomId}           # Rate limiting buckets
nickname:room:{roomId}             # Room-scoped nicknames (SET)
```

### Testing Strategy
- **Backend**: Unit tests for services, integration tests for REST/WebSocket
- **Frontend**: Component tests, integration tests for message flows
- **Scenarios**: Manual testing of key user journeys (defined in `docs/DEMO_CHECKLIST.md`)

### Performance Requirements
- Message delivery p95 latency ≤ 1 second
- Support 100 concurrent connections  
- First message sent within 60s of first visit
- Upload success rate ≥ 95%

## Project Status

Based on `todos.md`, all 29 major tasks are completed (100% completion rate). The project implements:

✅ **Core Features**: Guest auth, room creation/search, real-time messaging, media upload
✅ **Real-time**: WebSocket/STOMP, presence tracking, typing indicators  
✅ **Security**: Rate limiting, content sanitization, file validation, CSRF protection
✅ **Performance**: Caching, pagination, virtual scrolling, connection pooling
✅ **UX**: Responsive design, error handling, dark mode, upload previews
✅ **Infrastructure**: Docker setup, Nginx reverse proxy, database migrations
✅ **Documentation**: API docs, WebSocket specs, ERD, demo procedures