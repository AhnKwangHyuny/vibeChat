import axiosInstance from './axiosInstance';

interface RoomJoinRequest {
  nickname?: string;
  inviteCode?: string;
}

export const joinRoom = async (roomId: number, data: RoomJoinRequest): Promise<void> => {
  await axiosInstance.post(`/api/rooms/${roomId}/join`, data);
};
