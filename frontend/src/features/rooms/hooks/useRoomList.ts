/**
 * useRoomList Hook
 *
 * 방 목록 조회 및 무한 스크롤 관리
 * useInfiniteScroll 제네릭 훅을 활용한 도메인 특화 훅
 */

import { useCallback, useEffect } from 'react';
import { useInfiniteScroll } from '../../../hooks/common/useInfiniteScroll';
import { fetchRoomList } from '../api/roomListApi';
import { RoomListItemDto, RoomSortBy, RoomSortOrder } from '../../../types/roomList';

/**
 * useRoomList 훅 파라미터
 */
export interface UseRoomListParams {
  sortBy?: RoomSortBy;
  sortOrder?: RoomSortOrder;
  limit?: number;
  query?: string;
  tags?: string[];
  isPrivate?: boolean;
  autoLoad?: boolean; // 자동 초기 로드 여부 (기본값: true)
}

/**
 * useRoomList 훅 반환 타입
 */
export interface UseRoomListReturn {
  rooms: RoomListItemDto[];
  isLoading: boolean;
  isLoadingMore: boolean;
  hasMore: boolean;
  error: Error | null;
  loadMore: () => Promise<void>;
  refresh: () => Promise<void>;
}

/**
 * 방 목록 조회 훅
 *
 * @param params 정렬/필터/페이지네이션 옵션
 * @returns 방 목록 상태 및 메서드
 *
 * @example
 * ```tsx
 * const { rooms, loadMore, hasMore, isLoading } = useRoomList({
 *   sortBy: 'createdAt',
 *   sortOrder: 'DESC',
 *   limit: 20
 * });
 * ```
 */
export function useRoomList(params: UseRoomListParams = {}): UseRoomListReturn {
  const {
    sortBy = 'createdAt',
    sortOrder = 'DESC',
    limit = 20,
    query,
    tags,
    isPrivate,
    autoLoad = true,
  } = params;

  // Fetch 함수 정의
  const fetchFn = useCallback(
    async (cursor?: number | string) => {
      const response = await fetchRoomList({
        sortBy,
        sortOrder,
        limit,
        query,
        tags,
        isPrivate,
        lastId: cursor as number | undefined,
      });

      return {
        items: response.rooms,
        hasNext: response.hasNext,
        nextCursor: response.nextCursor,
      };
    },
    [sortBy, sortOrder, limit, query, tags, isPrivate]
  );

  // 제네릭 useInfiniteScroll 훅 활용
  const {
    items: rooms,
    isLoading,
    isLoadingMore,
    hasMore,
    error,
    loadMore,
    reset,
    refresh,
  } = useInfiniteScroll<RoomListItemDto>(fetchFn, { autoLoad: false });

  // 정렬/필터 옵션 변경 시 자동 리셋 및 재로드
  useEffect(() => {
    if (autoLoad) {
      reset();
      // reset 후 약간의 딜레이를 주고 로드
      const timer = setTimeout(() => {
        loadMore();
      }, 0);

      return () => clearTimeout(timer);
    }
  }, [sortBy, sortOrder, query, tags, isPrivate, autoLoad]);

  return {
    rooms,
    isLoading,
    isLoadingMore,
    hasMore,
    error,
    loadMore,
    refresh,
  };
}
