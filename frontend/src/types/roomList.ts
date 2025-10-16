/**
 * Room List Domain Types
 * 방 목록 조회 및 무한 스크롤 관련 타입 정의
 * 백엔드 API 응답과 1:1 매핑
 */

/**
 * 방 목록 아이템 DTO (백엔드 응답과 정확히 일치)
 */
export interface RoomListItemDto {
  id: number;
  title: string;
  description?: string;
  isPrivate: boolean;
  tags: string[];
  creatorNickname: string;
  participantsCount: number;
  maxParticipants?: number;
  createdAt: string; // ISO 8601 format
}

/**
 * 방 목록 페이지 응답 (백엔드 RoomListPageResponse와 일치)
 */
export interface RoomListPageDto {
  rooms: RoomListItemDto[];
  hasNext: boolean;
  nextCursor: number | null;
  size: number;
  totalCount: number | null;
}

/**
 * 방 목록 조회 API 요청 파라미터
 */
export interface RoomListQueryParams {
  query?: string;
  tags?: string[];
  isPrivate?: boolean;
  sortBy?: 'createdAt' | 'participantsCount';
  sortOrder?: 'DESC' | 'ASC';
  limit?: number;
  lastId?: number; // 커서 기반 페이지네이션
}

/**
 * 정렬 옵션 타입
 */
export type RoomSortBy = 'createdAt' | 'participantsCount';
export type RoomSortOrder = 'DESC' | 'ASC';

/**
 * 무한 스크롤 상태 (UI용)
 */
export interface InfiniteScrollState {
  isLoading: boolean;
  hasMore: boolean;
  error: Error | null;
}
