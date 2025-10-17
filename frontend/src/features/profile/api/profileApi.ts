/**
 * Profile API
 *
 * 책임: 백엔드 프로필 API 호출
 * - 실제 API 우선 사용
 * - API 실패 시 mock fallback (개발 환경만)
 */

import axiosInstance from '../../../services/api/axiosInstance';
import { ProfileResponse } from '../types/profile';
import { getMockProfile } from './mockProfileData';

/**
 * 내 프로필 조회
 *
 * GET /api/profile/me
 *
 * @returns 프로필 정보
 * @throws API 에러 (프로덕션 환경)
 *
 * 전략:
 * 1. 실제 API 호출 시도
 * 2. 성공 → API 데이터 반환
 * 3. 실패 (개발 환경) → Mock 데이터 fallback
 * 4. 실패 (프로덕션) → 에러 throw
 */
export const fetchMyProfile = async (): Promise<ProfileResponse> => {
  try {
    const response = await axiosInstance.get<ProfileResponse>('/profile/me');
    console.log('[ProfileAPI] API 데이터 로드 성공');
    return response.data;
  } catch (error) {
    // 개발 환경: Mock 데이터로 fallback
    if (process.env.NODE_ENV === 'development') {
      console.warn('[ProfileAPI] API 실패, Mock 데이터 사용 (개발 모드)');
      console.warn('에러:', error);

      // Mock 데이터 반환
      return getMockProfile();
    }

    // 프로덕션 환경: 에러 throw
    console.error('[ProfileAPI] API 호출 실패 (프로덕션)');
    throw error;
  }
};

// TODO: 향후 구현
// export const updateMyProfile = async (data: ProfileUpdateRequest) => { ... };
// export const deleteMyAvatar = async () => { ... };
