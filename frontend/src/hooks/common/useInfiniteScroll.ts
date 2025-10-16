/**
 * useInfiniteScroll Hook
 *
 * 제네릭 무한 스크롤 훅 - 다양한 리스트에 재사용 가능
 *
 * 사용 예시:
 * - 방 목록
 * - 메시지 목록
 * - 사용자 목록
 * - 검색 결과 목록
 *
 * @template T - 아이템 타입 (id 필드 필수)
 */

import { useState, useCallback, useRef } from 'react';

/**
 * 무한 스크롤 페이지 응답 인터페이스
 */
export interface InfiniteScrollPageResponse<T> {
  items: T[];
  hasNext: boolean;
  nextCursor: number | string | null;
}

/**
 * Fetch 함수 타입
 *
 * @param cursor - 페이지네이션 커서 (첫 페이지는 undefined)
 * @returns 페이지 응답
 */
export type FetchFunction<T> = (cursor?: number | string) => Promise<InfiniteScrollPageResponse<T>>;

/**
 * useInfiniteScroll 반환 타입
 */
export interface UseInfiniteScrollReturn<T> {
  items: T[];
  isLoading: boolean;
  isLoadingMore: boolean;
  hasMore: boolean;
  error: Error | null;
  loadMore: () => Promise<void>;
  reset: () => void;
  refresh: () => Promise<void>;
}

/**
 * 무한 스크롤 커스텀 훅
 *
 * @param fetchFn - 데이터 페칭 함수
 * @param options - 옵션 (자동 초기 로드 여부)
 * @returns 무한 스크롤 상태 및 메서드
 */
export function useInfiniteScroll<T>(
  fetchFn: FetchFunction<T>,
  options: { autoLoad?: boolean } = { autoLoad: true }
): UseInfiniteScrollReturn<T> {
  const [items, setItems] = useState<T[]>([]);
  const [cursor, setCursor] = useState<number | string | undefined>(undefined);
  const [hasMore, setHasMore] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  // 중복 요청 방지를 위한 ref
  const isLoadingRef = useRef(false);
  const isMountedRef = useRef(true);

  /**
   * 데이터 로드 (초기 로드 또는 추가 로드)
   */
  const loadMore = useCallback(async () => {
    // 중복 요청 방지
    if (isLoadingRef.current || !hasMore) {
      return;
    }

    // 초기 로드인지 추가 로드인지 구분
    const isInitialLoad = items.length === 0 && cursor === undefined;

    try {
      isLoadingRef.current = true;

      if (isInitialLoad) {
        setIsLoading(true);
      } else {
        setIsLoadingMore(true);
      }

      setError(null);

      const result = await fetchFn(cursor);

      // 컴포넌트가 unmount된 경우 상태 업데이트 방지
      if (!isMountedRef.current) return;

      setItems((prev) => [...prev, ...result.items]);
      setCursor(result.nextCursor ?? undefined);
      setHasMore(result.hasNext);
    } catch (err) {
      if (!isMountedRef.current) return;

      const error = err instanceof Error ? err : new Error('Failed to load data');
      setError(error);
      console.error('[useInfiniteScroll] Load error:', error);
    } finally {
      if (isMountedRef.current) {
        setIsLoading(false);
        setIsLoadingMore(false);
        isLoadingRef.current = false;
      }
    }
  }, [fetchFn, cursor, hasMore, items.length]);

  /**
   * 리셋 (초기 상태로 되돌리기)
   */
  const reset = useCallback(() => {
    setItems([]);
    setCursor(undefined);
    setHasMore(true);
    setError(null);
    setIsLoading(false);
    setIsLoadingMore(false);
    isLoadingRef.current = false;
  }, []);

  /**
   * 새로고침 (현재 데이터를 유지하면서 첫 페이지부터 다시 로드)
   */
  const refresh = useCallback(async () => {
    reset();

    // reset 후 약간의 딜레이를 주고 로드
    await new Promise((resolve) => setTimeout(resolve, 0));
    await loadMore();
  }, [reset, loadMore]);

  // 컴포넌트 unmount 시 flag 설정
  useState(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  });

  return {
    items,
    isLoading,
    isLoadingMore,
    hasMore,
    error,
    loadMore,
    reset,
    refresh,
  };
}
