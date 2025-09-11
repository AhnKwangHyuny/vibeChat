import api from './axiosInstance';

export interface GuestCreateReq { nickname: string }
export interface GuestCreateRes { userId: number; nickname: string; avatarUrl?: string }
export interface MeRes { userId: number; nickname: string; provider: 'GUEST' | 'GOOGLE'; avatarUrl?: string }

export async function createGuestUser(data: GuestCreateReq): Promise<GuestCreateRes> {
  const res = await api.post<GuestCreateRes>('/users/guest', data);
  return res.data;
}

export async function getMe(): Promise<MeRes> {
  const res = await api.get<MeRes>('/auth/me');
  return res.data;
}

export async function logout(): Promise<void> {
  await api.post('/auth/logout');
}
