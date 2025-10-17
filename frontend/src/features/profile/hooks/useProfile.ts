/**
 * useProfile Hook
 *
 * 책임: 프로필 데이터 조회 및 상태 관리
 * - React Query 기반 서버 상태 관리
 * - 로딩, 에러, 성공 상태 관리
 * - 5분 캐싱
 */

import { useQuery } from '@tanstack/react-query';
import { fetchMyProfile } from '../api/profileApi';
import { ProfileResponse } from '../types/profile';

/**
 * 프로필 조회 훅
 *
 * @returns {object} 프로필 데이터 및 상태
 * @property {ProfileResponse | undefined} data - 프로필 데이터
 * @property {boolean} isLoading - 로딩 중 여부
 * @property {boolean} isError - 에러 발생 여부
 * @property {Error | null} error - 에러 객체
 * @property {function} refetch - 수동 재조회 함수
 *
 * @example
 * ```tsx
 * const { data: profile, isLoading, isError, error } = useProfile();
 *
 * if (isLoading) return <div>Loading...</div>;
 * if (isError) return <div>Error: {error.message}</div>;
 * if (!profile) return null;
 *
 * return <div>{profile.nickname}</div>;
 * ```
 */
export function useProfile() {
  return useQuery<ProfileResponse, Error>({
    queryKey: ['profile', 'me'],
    queryFn: fetchMyProfile,
    staleTime: 5 * 60 * 1000, // 5분 (fresh 상태 유지)
    gcTime: 10 * 60 * 1000, // 10분 (캐시 보관 시간)
    retry: 1, // API 실패 시 1번만 재시도
    refetchOnWindowFocus: false, // 윈도우 포커스 시 자동 재조회 비활성화
  });
}
