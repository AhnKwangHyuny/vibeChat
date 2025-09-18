/**
 * Room Domain Types
 * 방(Room) 관련 도메인 모델 정의
 */

// 방 공개 설정
export type RoomPrivacy = 'PUBLIC' | 'PRIVATE';

// 방 상태
export type RoomStatus = 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';

// 방 생성 요청 데이터
export interface CreateRoomRequest {
  title: string;
  description?: string;
  isPrivate: boolean;
  tags: string[];
}

// 방 생성 응답 데이터
export interface CreateRoomResponse {
  id: number;
  title: string;
  description?: string;
  isPrivate: boolean;
  tags: string[];
  participantsCount: number;
  lastMessageAt?: string;
  inviteCode?: string;
}

// 방 정보 (상세)
export interface Room {
  id: number;
  title: string;
  description?: string;
  isPrivate: boolean;
  tags: string[];
  creatorId: string;
  creatorNickname: string;
  participantsCount: number;
  maxParticipants?: number;
  createdAt: string;
  updatedAt: string;
  status: RoomStatus;
  inviteCode?: string; // 비공개 방의 경우
}

// 방 목록 아이템 (간소화된 정보)
export interface RoomListItem {
  id: number;
  title: string;
  description?: string;
  isPrivate: boolean;
  tags: string[];
  creatorNickname: string;
  participantsCount: number;
  maxParticipants?: number;
  createdAt: string;
  status: RoomStatus;
}

// 방 참여자 정보
export interface RoomParticipant {
  userId: string;
  nickname: string;
  avatarUrl?: string;
  joinedAt: string;
  isOnline: boolean;
  role: 'CREATOR' | 'ADMIN' | 'MEMBER';
}

// 방 검색 필터
export interface RoomSearchFilter {
  query?: string;
  tags?: string[];
  isPrivate?: boolean;
  status?: RoomStatus;
  sortBy?: 'createdAt' | 'participantsCount' | 'title';
  sortOrder?: 'ASC' | 'DESC';
  limit?: number;
  offset?: number;
}

// 방 참여 요청
export interface JoinRoomRequest {
  id: number;
  inviteCode?: string; // 비공개 방의 경우
}

// 방 참여 응답
export interface JoinRoomResponse {
  success: boolean;
  message: string;
  room?: Room;
}

// 방 나가기 요청
export interface LeaveRoomRequest {
  id: number;
}

// 방 나가기 응답
export interface LeaveRoomResponse {
  success: boolean;
  message: string;
}

// 방 업데이트 요청
export interface UpdateRoomRequest {
  id: number;
  title?: string;
  description?: string;
  tags?: string[];
  maxParticipants?: number;
}

// 방 업데이트 응답
export interface UpdateRoomResponse {
  success: boolean;
  message: string;
  room?: Room;
}

// 방 삭제 요청
export interface DeleteRoomRequest {
  id: number;
}

// 방 삭제 응답
export interface DeleteRoomResponse {
  success: boolean;
  message: string;
}
