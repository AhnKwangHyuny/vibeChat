import axiosInstance from './axiosInstance';
import { 
  CreateRoomRequest, 
  CreateRoomResponse, 
  Room, 
  RoomListItem, 
  RoomSearchFilter,
  JoinRoomRequest,
  JoinRoomResponse,
  LeaveRoomRequest,
  LeaveRoomResponse,
  UpdateRoomRequest,
  UpdateRoomResponse,
  DeleteRoomRequest,
  DeleteRoomResponse
} from '../../types';

/**
 * 방 생성
 */
export const createRoom = async (data: CreateRoomRequest): Promise<CreateRoomResponse> => {
  // 백엔드 응답은 'private' 필드를 포함하므로, 응답 타입을 any로 지정합니다.
  const response = await axiosInstance.post<any>('/rooms', data);
  
  // 백엔드 응답(private)과 프론트엔드 모델(isPrivate) 간의 불일치를 수동으로 해결합니다.
  const responseData = response.data;
  
  const newRoom: CreateRoomResponse = {
    id: responseData.id,
    title: responseData.title,
    description: responseData.description,
    isPrivate: responseData.private, // 'private'을 'isPrivate'으로 매핑합니다.
    tags: responseData.tags,
    participantsCount: responseData.participantsCount,
    lastMessageAt: responseData.lastMessageAt,
    inviteCode: responseData.inviteCode,
  };
  
  return newRoom;
};

/**
 * 방 상세 정보 조회
 */
export const getRoomById = async (roomId: number): Promise<Room> => {
  const response = await axiosInstance.get<any>(`/rooms/${roomId}`);

  // 백엔드 응답과 프론트엔드 타입 매핑
  const responseData = response.data;
  const room: Room = {
    id: responseData.id,
    title: responseData.title,
    description: responseData.description,
    isPrivate: responseData.private, // 'private'을 'isPrivate'으로 매핑
    tags: responseData.tags,
    participantsCount: responseData.participantsCount,
    lastMessageAt: responseData.lastMessageAt,
    inviteCode: responseData.inviteCode,
  };

  return room;
};

/**
 * 방 목록 조회
 */
export const getRooms = async (filter?: RoomSearchFilter): Promise<RoomListItem[]> => {
  const response = await axiosInstance.get<RoomListItem[]>('/rooms', { params: filter });
  return response.data;
};

/**
 * 방 검색
 */
export const searchRooms = async (filter: RoomSearchFilter): Promise<RoomListItem[]> => {
  const response = await axiosInstance.get<RoomListItem[]>('/rooms/search', { params: filter });
  return response.data;
};

/**
 * 방 참여
 */
export const joinRoom = async (data: JoinRoomRequest): Promise<JoinRoomResponse> => {
  const response = await axiosInstance.post<JoinRoomResponse>(`/rooms/${data.roomId}/join`, {
    inviteCode: data.inviteCode
  });
  return response.data;
};

/**
 * 방 나가기
 */
export const leaveRoom = async (data: LeaveRoomRequest): Promise<LeaveRoomResponse> => {
  const response = await axiosInstance.post<LeaveRoomResponse>(`/rooms/${data.roomId}/leave`);
  return response.data;
};

/**
 * 방 정보 수정
 */
export const updateRoom = async (data: UpdateRoomRequest): Promise<UpdateRoomResponse> => {
  const response = await axiosInstance.put<UpdateRoomResponse>(`/rooms/${data.roomId}`, data);
  return response.data;
};

/**
 * 방 삭제
 */
export const deleteRoom = async (data: DeleteRoomRequest): Promise<DeleteRoomResponse> => {
  const response = await axiosInstance.delete<DeleteRoomResponse>(`/rooms/${data.roomId}`);
  return response.data;
};
