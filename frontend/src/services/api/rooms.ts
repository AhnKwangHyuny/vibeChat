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
  const response = await axiosInstance.post<CreateRoomResponse>('/rooms', data);
  return response.data;
};

/**
 * 방 상세 정보 조회
 */
export const getRoomById = async (roomId: number): Promise<Room> => {
  const response = await axiosInstance.get<Room>(`/rooms/${roomId}`);
  return response.data;
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
