import { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import { getRoomById as apiGetRoomById } from '../services/api/rooms';

interface Room {
  id: number;
  title: string;
  description?: string;
  isPrivate: boolean;
  tags: string[];
  participantsCount: number;
  lastMessageAt?: string;
  inviteCode?: string;
}

interface UseRoomsReturn {
  rooms: Room[];
  isLoading: boolean;
  searchRooms: (tags: string[]) => Promise<void>;
  createRoom: (roomData: Omit<Room, 'id' | 'participantsCount' | 'lastMessageAt'>) => Promise<Room>;
  joinRoom: (roomId: number, inviteCode?: string) => Promise<void>;
  getRoomById: (roomId: number) => Promise<Room | null>;
}

// Mock data for development
const mockRooms: Room[] = [
  {
    id: 1,
    title: "React Developers",
    description: "Discuss React and modern web development",
    tags: ["react", "javascript", "frontend"],
    participantsCount: 15,
    isPrivate: false,
    lastMessageAt: new Date().toISOString()
  },
  {
    id: 2,
    title: "TypeScript Enthusiasts",
    description: "TypeScript tips and tricks",
    tags: ["typescript", "javascript", "programming"],
    participantsCount: 8,
    isPrivate: false,
    lastMessageAt: new Date(Date.now() - 3600000).toISOString()
  },
  {
    id: 3,
    title: "Design Systems",
    description: "Building consistent UI components",
    tags: ["design", "ui", "components"],
    participantsCount: 12,
    isPrivate: true,
    inviteCode: "DESIGN2024",
    lastMessageAt: new Date(Date.now() - 7200000).toISOString()
  },
  {
    id: 4,
    title: "VibeChat Community",
    description: "General discussion about VibeChat platform",
    tags: ["community", "general", "chat"],
    participantsCount: 25,
    isPrivate: false,
    lastMessageAt: new Date(Date.now() - 1800000).toISOString()
  },
  {
    id: 5,
    title: "Web Development",
    description: "Full-stack web development discussions",
    tags: ["web", "development", "fullstack"],
    participantsCount: 18,
    isPrivate: false,
    lastMessageAt: new Date(Date.now() - 900000).toISOString()
  }
];

export function useRooms(): UseRoomsReturn {
  const [rooms, setRooms] = useState<Room[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const searchRooms = async (tags: string[]): Promise<void> => {
    setIsLoading(true);
    
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    // Filter rooms based on tags
    const filteredRooms = mockRooms.filter(room =>
      tags.some(tag => room.tags.some(roomTag => 
        roomTag.toLowerCase().includes(tag.toLowerCase())
      ))
    );
    
    setRooms(filteredRooms);
    setIsLoading(false);
  };

  const createRoom = async (roomData: Omit<Room, 'id' | 'participantsCount' | 'lastMessageAt'>): Promise<Room> => {
    setIsLoading(true);
    
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    const newRoom: Room = {
      id: Date.now(),
      ...roomData,
      participantsCount: 1,
      lastMessageAt: new Date().toISOString(),
      inviteCode: roomData.isPrivate ? `INVITE${Date.now()}` : undefined,
    };
    
    // Add to mock data
    mockRooms.unshift(newRoom);
    
    setIsLoading(false);
    toast.success(`Room '${newRoom.title}' created successfully!`);
    
    return newRoom;
  };

  const joinRoom = async (roomId: number, inviteCode?: string): Promise<void> => {
    setIsLoading(true);
    
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 500));
    
    const room = mockRooms.find(r => r.id === roomId);
    if (!room) {
      throw new Error('Room not found');
    }
    
    if (room.isPrivate && room.inviteCode !== inviteCode) {
      throw new Error('Invalid invite code');
    }
    
    // Simulate joining room
    room.participantsCount += 1;
    
    setIsLoading(false);
    toast.success('Successfully joined room!');
  };

  const getRoomById = useCallback(async (roomId: number): Promise<Room | null> => {
    setIsLoading(true);

    try {
      const room = await apiGetRoomById(roomId);
      setIsLoading(false);
      return room;
    } catch (error) {
      console.error('Failed to fetch room:', error);
      setIsLoading(false);

      // API 실패 시 fallback으로 mock 데이터 사용
      const room = mockRooms.find(r => r.id === roomId) || null;
      return room;
    }
  }, []);

  return {
    rooms,
    isLoading,
    searchRooms,
    createRoom,
    joinRoom,
    getRoomById,
  };
}
