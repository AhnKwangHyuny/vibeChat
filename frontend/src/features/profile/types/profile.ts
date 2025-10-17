/**
 * Profile Types
 *
 * 책임: 프로필 관련 타입 정의
 * - API 응답 타입 (백엔드 스키마와 일치)
 * - UI 컴포넌트 타입
 * - 변환 함수
 */

/**
 * 백엔드 API 응답 타입
 * GET /api/profile/me
 */
export interface ProfileResponse {
  userId: string;
  nickname: string;
  email: string;
  avatarUrl: string | null;
  greeting: string | null;
  joinedAt: string; // ISO 8601 format
  status: 'online' | 'away' | 'offline';
  userType: 'GUEST' | 'GOOGLE';
  stats: {
    totalMessages: number;
    roomsJoined: number;
  };
}

/**
 * ProfileCard 컴포넌트가 요구하는 타입
 * (기존 components/demo/ProfileCard.tsx와 호환)
 */
export interface ProfileCardData {
  id: string;
  nickname: string;
  email?: string;
  avatarUrl?: string;
  bio?: string; // greeting → bio
  joinedAt: string;
  status: 'online' | 'away' | 'offline';
  badges: string[]; // 향후 추가 (현재는 빈 배열)
  stats: {
    totalMessages: number;
    roomsJoined: number;
    friendsCount: number; // 향후 추가 (현재는 0)
  };
}

/**
 * API 응답 → UI 컴포넌트 데이터 변환
 *
 * SRP: 변환 로직을 별도 함수로 분리
 *
 * @param profile 백엔드 API 응답
 * @returns ProfileCard에서 사용할 수 있는 데이터
 */
export function toProfileCardData(profile: ProfileResponse): ProfileCardData {
  return {
    id: profile.userId,
    nickname: profile.nickname,
    email: profile.email,
    avatarUrl: profile.avatarUrl || undefined,
    bio: profile.greeting || undefined,
    joinedAt: profile.joinedAt,
    status: profile.status,
    badges: [], // TODO: 향후 뱃지 시스템 구현 시 추가
    stats: {
      totalMessages: profile.stats.totalMessages,
      roomsJoined: profile.stats.roomsJoined,
      friendsCount: 0, // TODO: 향후 친구 시스템 구현 시 추가
    },
  };
}
