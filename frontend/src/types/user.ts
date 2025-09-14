/**
 * User Domain Types
 * 사용자(User) 관련 도메인 모델 정의
 */

// 사용자 제공자 (로그인 방식)
export type UserProvider = 'GUEST' | 'GOOGLE';

// 사용자 상태
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

// 사용자 정보 (기본)
export interface User {
  id: string;
  nickname: string;
  avatarUrl?: string;
  provider: UserProvider;
  status: UserStatus;
  createdAt: string;
  updatedAt: string;
  lastActiveAt?: string;
}

// 사용자 생성 요청 (게스트)
export interface CreateGuestUserRequest {
  nickname: string;
}

// 사용자 생성 응답 (게스트)
export interface CreateGuestUserResponse {
  userId: number;
  nickname: string;
  provider: UserProvider;
  createdAt: string;
}

// 사용자 생성 요청 (Google)
export interface CreateGoogleUserRequest {
  googleId: string;
  email: string;
  nickname: string;
  avatarUrl?: string;
}

// 사용자 생성 응답 (Google)
export interface CreateGoogleUserResponse {
  userId: number;
  nickname: string;
  email: string;
  avatarUrl?: string;
  provider: UserProvider;
  createdAt: string;
}

// 사용자 정보 업데이트 요청
export interface UpdateUserRequest {
  nickname?: string;
  avatarUrl?: string;
}

// 사용자 정보 업데이트 응답
export interface UpdateUserResponse {
  success: boolean;
  message: string;
  user?: User;
}

// 사용자 프로필 정보
export interface UserProfile {
  id: string;
  nickname: string;
  avatarUrl?: string;
  provider: UserProvider;
  status: UserStatus;
  createdAt: string;
  updatedAt: string;
  lastActiveAt?: string;
  // 통계 정보
  roomsCreated: number;
  roomsJoined: number;
  messagesSent: number;
}

// 사용자 검색 필터
export interface UserSearchFilter {
  query?: string;
  provider?: UserProvider;
  status?: UserStatus;
  sortBy?: 'nickname' | 'createdAt' | 'lastActiveAt';
  sortOrder?: 'ASC' | 'DESC';
  limit?: number;
  offset?: number;
}

// 사용자 세션 정보
export interface UserSession {
  userId: string;
  nickname: string;
  provider: UserProvider;
  isAuthenticated: boolean;
  expiresAt?: string;
}
