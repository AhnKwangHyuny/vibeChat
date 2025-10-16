/**
 * RoomListGrid Component
 * 방 목록 그리드 레이아웃
 * Home.tsx 371-386줄의 기존 디자인 유지
 */

import { RoomCard } from '../../../components/demo/RoomCard';
import NotificationBadge from '../../../components/ui/NotificationBadge';
import { RoomListItemDto } from '../../../types/roomList';

interface RoomListGridProps {
  rooms: RoomListItemDto[];
  onJoinRoom: (roomId: number, isPrivate: boolean) => void;
}

export function RoomListGrid({ rooms, onJoinRoom }: RoomListGridProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {rooms.map((room) => (
        <NotificationBadge
          key={room.id}
          count={Math.floor(Math.random() * 5) + 1}
          variant="danger"
          position="top-right"
        >
          <RoomCard
            id={room.id.toString()}
            title={room.title}
            description={room.description}
            tags={room.tags}
            isPrivate={room.isPrivate}
            participantsCount={room.participantsCount}
            lastMessageAt={room.createdAt}
            onJoin={() => onJoinRoom(room.id, room.isPrivate)}
          />
        </NotificationBadge>
      ))}
    </div>
  );
}
