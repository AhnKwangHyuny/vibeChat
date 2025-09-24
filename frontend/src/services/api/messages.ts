import axiosInstance from './axiosInstance';

interface UserSummaryDto {
  id: number;
  nickname: string;
  avatarUrl?: string;
}

interface WebSocketMessageResponse {
  id: number;
  clientTempId?: string;
  roomId: number;
  user: UserSummaryDto;
  type: 'TEXT' | 'IMAGE' | 'GIF' | 'VIDEO';
  contentText?: string;
  mediaUrl?: string;
  mediaThumbUrl?: string;
  mediaDurationSec?: number;
  createdAt: string;
}

export const getMessages = async (roomId: number, beforeId?: number, limit: number = 30): Promise<WebSocketMessageResponse[]> => {
  const response = await axiosInstance.get<WebSocketMessageResponse[]>(`/messages/${roomId}`, {
    params: { beforeId, limit },
  });

  console.log('Messages fetched from API-Server:', response.data);
  return response.data;
};
