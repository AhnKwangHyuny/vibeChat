/**
 * Mock Profile Data
 *
 * 테스트용 Mock 데이터
 * 나중에 이 파일만 삭제하면 됨
 *
 * 책임: 테스트/개발용 더미 데이터만 제공
 * - 비즈니스 로직 없음
 * - API가 없을 때 fallback으로 사용
 */

import { ProfileResponse } from '../types/profile';

/**
 * Mock 프로필 데이터 (게스트)
 */
export const MOCK_GUEST_PROFILE: ProfileResponse = {
  userId: '1',
  nickname: 'TestGuest',
  email: 'guest@gmail.com',
  avatarUrl: null,
  greeting: '안녕하세요! 게스트 사용자입니다.',
  joinedAt: '2024-01-15T00:00:00Z',
  status: 'online',
  userType: 'GUEST',
  stats: {
    totalMessages: 0,
    roomsJoined: 0,
  },
};

/**
 * Mock 프로필 데이터 (Google 사용자)
 */
export const MOCK_GOOGLE_PROFILE: ProfileResponse = {
  userId: '2',
  nickname: 'John Doe',
  email: 'john.doe@gmail.com',
  avatarUrl: 'https://lh3.googleusercontent.com/a/default-user',
  greeting: '열정적인 개발자이자 커뮤니티 빌더입니다. React와 TypeScript를 사랑하며, 좋은 UX를 만드는 것에 관심이 많습니다.',
  joinedAt: '2024-01-10T00:00:00Z',
  status: 'online',
  userType: 'GOOGLE',
  stats: {
    totalMessages: 1247,
    roomsJoined: 12,
  },
};

/**
 * Mock 데이터 반환
 *
 * 실제 환경에서는 사용자 provider에 따라 다른 mock 반환
 * (현재는 GOOGLE mock 반환)
 */
export function getMockProfile(): ProfileResponse {
  // TODO: 실제로는 현재 세션의 userType에 따라 분기
  return MOCK_GOOGLE_PROFILE;
}
