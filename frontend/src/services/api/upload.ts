import axiosInstance from './axiosInstance';

interface UploadResponse {
  type: 'TEXT' | 'IMAGE' | 'GIF' | 'VIDEO';
  url: string;
  thumbUrl?: string;
  durationSec?: number;
}

export const uploadFile = async (file: File): Promise<UploadResponse> => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await axiosInstance.post<UploadResponse>('/api/upload/media', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};
