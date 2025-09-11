import axiosInstance from './axiosInstance';

interface TagResponse {
  name: string;
  popularity: number;
}

export const autocompleteTags = async (query: string): Promise<TagResponse[]> => {
  const response = await axiosInstance.get<TagResponse[]>(`/api/tags/autocomplete`, { params: { q: query } });
  return response.data;
};
