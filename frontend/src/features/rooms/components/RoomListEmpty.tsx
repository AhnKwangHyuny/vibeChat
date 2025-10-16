/**
 * RoomListEmpty Component
 * 방 목록이 비어있을 때 표시되는 UI
 * Home.tsx 364-369줄의 기존 디자인 유지
 */

import { Card } from '../../../components/ui/Card';
import { Button } from '../../../components/ui/Button';

interface RoomListEmptyProps {
  onCreateRoom: () => void;
}

export function RoomListEmpty({ onCreateRoom }: RoomListEmptyProps) {
  return (
    <Card className="p-12 text-center">
      <div className="text-6xl mb-4">🔍</div>
      <h4 className="text-xl font-semibold text-foreground-primary mb-2">No rooms found</h4>
      <p className="text-foreground-muted mb-6">Try different tags or create a new room for this topic</p>
      <Button onClick={onCreateRoom}>Create New Room</Button>
    </Card>
  );
}
