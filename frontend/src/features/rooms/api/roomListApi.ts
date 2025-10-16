/**
 * Room List API
 * 방 목록 조회 및 무한 스크롤 관련 API 호출
 */

import axiosInstance from '../../../services/api/axiosInstance';
import { RoomListQueryParams, RoomListPageDto } from '../../../types/roomList';

/**
 * 방 목록 조회 (커서 기반 페이지네이션)
 *
 * GET /api/rooms
 *
 * @param params 검색/필터/정렬/페이지네이션 파라미터
 * @returns 방 목록 페이지 응답
 */
export const fetchRoomList = async (params: RoomListQueryParams): Promise<RoomListPageDto> => {
  const response = await axiosInstance.get<RoomListPageDto>('/rooms', {
    params: {
      query: params.query,
      tags: params.tags,
      isPrivate: params.isPrivate,
      sortBy: params.sortBy || 'createdAt',
      sortOrder: params.sortOrder || 'DESC',
      limit: params.limit || 20,
      lastId: params.lastId, // 커서 (무한 스크롤)
    },
  });

  return response.data;
};

/**
 * 최신 방 목록 조회
 *
 * GET /api/rooms/recent
 *
 * @param limit 조회 개수 (기본값: 20)
 * @returns 최신 방 목록
 */
export const fetchRecentRooms = async (limit: number = 20): Promise<RoomListPageDto> => {
  const response = await axiosInstance.get<RoomListPageDto>('/rooms/recent', {
    params: { limit },
  });

  return response.data;
};

/**
 * 인기 방 목록 조회
 *
 * GET /api/rooms/popular
 *
 * @param limit 조회 개수 (기본값: 20)
 * @returns 인기 방 목록
 */
export const fetchPopularRooms = async (limit: number = 20): Promise<RoomListPageDto> => {
  const response = await axiosInstance.get<RoomListPageDto>('/rooms/popular', {
    params: { limit },
  });

  return response.data;
};

/**
 * 태그 기반 방 검색
 *
 * GET /api/rooms/search
 *
 * @param tags 검색할 태그 배열
 * @param lastId 커서 (무한 스크롤)
 * @param limit 조회 개수 (기본값: 20)
 * @returns 태그에 해당하는 방 목록
 */
export const searchRoomsByTags = async (
  tags: string[],
  lastId?: number,
  limit: number = 20
): Promise<RoomListPageDto> => {
  const response = await axiosInstance.get<RoomListPageDto>('/rooms/search', {
    params: {
      tags,
      lastId,
      limit,
    },
  });

  return response.data;
};
