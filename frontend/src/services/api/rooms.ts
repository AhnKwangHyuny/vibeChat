import axiosInstance from './axiosInstance';

interface RoomCreateRequest {
  title: string;
  description?: string;
  isPrivate: boolean;
  tags: string[];
}

interface RoomResponse {
  id: number;
  title: string;
  description?: string;
  isPrivate: boolean;
  tags: string[];
  participantsCount: number;
  lastMessageAt?: string;
  inviteCode?: string;
}

export const createRoom = async (data: RoomCreateRequest): Promise<RoomResponse> => {
  const response = await axiosInstance.post<RoomResponse>('/api/rooms', data);
  return response.data;
};

export const getRoomById = async (roomId: number): Promise<RoomResponse> => {
  const response = await axiosInstance.get<RoomResponse>(`/api/rooms/${roomId}`);
  return response.data;
};

export const searchRooms = async (tags: string[]): Promise<RoomResponse[]> => {
  const response = await axiosInstance.get<RoomResponse[]>(`/api/rooms/search`, { params: { tags } });
  return response.data;
};
