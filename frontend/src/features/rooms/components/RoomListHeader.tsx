/**
 * RoomListHeader Component
 * 방 목록 헤더 (제목, 결과 개수, 정렬, 새 방 만들기 버튼)
 * Home.tsx 311-344줄의 기존 디자인 유지
 */

import { Button } from '../../../components/ui/Button';
import { RoomSortControls, SortOption } from './RoomSortControls';

interface RoomListHeaderProps {
  roomCount: number;
  sortBy: SortOption;
  onSortChange: (sortBy: SortOption) => void;
  onCreateRoom: () => void;
}

export function RoomListHeader({ roomCount, sortBy, onSortChange, onCreateRoom }: RoomListHeaderProps) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-6 sm:mb-8 gap-4">
      <div>
        <h3 className="text-xl sm:text-2xl font-bold text-foreground-primary mb-2">
          사용 가능한 방
        </h3>
        <p className="text-sm sm:text-base text-foreground-muted">
          검색 결과 {roomCount}개의 방을 찾았습니다
        </p>
      </div>
      <div className="flex flex-col sm:flex-row gap-3 sm:gap-4">
        <RoomSortControls value={sortBy} onChange={onSortChange} />
        <Button variant="secondary" onClick={onCreateRoom} className="flex items-center gap-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          새 방 만들기
        </Button>
      </div>
    </div>
  );
}
