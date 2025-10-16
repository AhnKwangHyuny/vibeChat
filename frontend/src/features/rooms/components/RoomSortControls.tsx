/**
 * RoomSortControls Component
 * 방 목록 정렬 드롭다운 컨트롤
 * Home.tsx 322-336줄의 기존 디자인 유지
 */

import { RoomSortBy, RoomSortOrder } from '../../../types/roomList';

export type SortOption = 'newest' | 'oldest' | 'participants';

interface RoomSortControlsProps {
  value: SortOption;
  onChange: (value: SortOption) => void;
}

export function RoomSortControls({ value, onChange }: RoomSortControlsProps) {
  return (
    <div className="relative">
      <select
        value={value}
        onChange={(e) => onChange(e.target.value as SortOption)}
        className="appearance-none bg-background-secondary border border-border-default rounded-base px-4 py-2 pr-8 text-sm text-foreground-primary focus:outline-none focus:ring-2 focus:ring-border-focus focus:border-border-focus"
      >
        <option value="newest">최신순</option>
        <option value="oldest">오래된순</option>
        <option value="participants">인원수순</option>
      </select>
      <div className="absolute inset-y-0 right-0 flex items-center pr-2 pointer-events-none">
        <svg className="w-4 h-4 text-foreground-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </div>
    </div>
  );
}

/**
 * SortOption을 API 파라미터로 변환하는 유틸리티 함수
 */
export function convertSortOptionToApi(option: SortOption): { sortBy: RoomSortBy; sortOrder: RoomSortOrder } {
  switch (option) {
    case 'newest':
      return { sortBy: 'createdAt', sortOrder: 'DESC' };
    case 'oldest':
      return { sortBy: 'createdAt', sortOrder: 'ASC' };
    case 'participants':
      return { sortBy: 'participantsCount', sortOrder: 'DESC' };
  }
}
