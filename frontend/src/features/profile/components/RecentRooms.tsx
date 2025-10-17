/**
 * RecentRooms Component
 *
 * 책임: 최근 활동한 방 목록 표시
 * - RoomListCard 재사용
 * - Grid 레이아웃 (1열 → 2열 XL)
 */

import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { Card } from '../../../components/ui/Card';
import RoomListCard from '../../../components/demo/RoomListCard';

interface Room {
  id: number;
  title: string;
  description: string;
  tags: string[];
  participantsCount: number;
  isPrivate: boolean;
  lastMessageAt: string;
  lastMessage?: {
    user: string;
    content: string;
  };
  unreadCount?: number;
  isMuted?: boolean;
  isTyping?: boolean;
  typingUsers?: string[];
}

interface RecentRoomsProps {
  rooms: Room[];
}

export function RecentRooms({ rooms }: RecentRoomsProps) {
  const navigate = useNavigate();

  const handleJoinRoom = (roomId: number) => {
    navigate(`/room/${roomId}`);
  };

  const handleLeaveRoom = (title: string) => {
    toast.success(`${title} 방에서 나왔습니다.`);
  };

  const handleMuteRoom = (title: string) => {
    toast.success(`${title} 방의 알림을 껐습니다.`);
  };

  const handleUnmuteRoom = (title: string) => {
    toast.success(`${title} 방의 알림을 켰습니다.`);
  };

  return (
    <Card>
      <div className="p-6">
        <h3 className="text-lg font-semibold text-foreground-primary mb-4">
          최근 활동한 방
        </h3>
        <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
          {rooms.map((room) => (
            <RoomListCard
              key={room.id}
              room={room}
              onJoin={() => handleJoinRoom(room.id)}
              onLeave={() => handleLeaveRoom(room.title)}
              onMute={() => handleMuteRoom(room.title)}
              onUnmute={() => handleUnmuteRoom(room.title)}
            />
          ))}
        </div>
      </div>
    </Card>
  );
}
