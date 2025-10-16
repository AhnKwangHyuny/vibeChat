/**
 * RoomList Component
 * 무한 스크롤 기반 방 목록 컨테이너
 */

import { useInView } from 'react-intersection-observer';
import { useRoomList } from '../hooks/useRoomList';
import { RoomListSkeleton } from './RoomListSkeleton';
import { RoomListEmpty } from './RoomListEmpty';
import { RoomListGrid } from './RoomListGrid';
import { RoomListHeader } from './RoomListHeader';
import { SortOption, convertSortOptionToApi } from './RoomSortControls';
import LoadingDots from '../../../components/ui/LoadingDots';
import { useState } from 'react';

interface RoomListProps {
  onJoinRoom: (roomId: number, isPrivate: boolean) => void;
  onCreateRoom: () => void;
}

export function RoomList({ onJoinRoom, onCreateRoom }: RoomListProps) {
  const [sortOption, setSortOption] = useState<SortOption>('newest');

  // SortOption을 API 파라미터로 변환
  const { sortBy, sortOrder } = convertSortOptionToApi(sortOption);

  // 방 목록 조회
  const { rooms, isLoading, isLoadingMore, hasMore, error, loadMore } = useRoomList({
    sortBy,
    sortOrder,
    limit: 20,
    autoLoad: true,
  });

  // Intersection Observer로 무한 스크롤 트리거
  const { ref: loadMoreRef } = useInView({
    threshold: 0.5,
    onChange: (inView) => {
      if (inView && hasMore && !isLoading && !isLoadingMore) {
        loadMore();
      }
    },
  });

  // 초기 로딩 상태
  if (isLoading && rooms.length === 0) {
    return (
      <div>
        <RoomListHeader
          roomCount={0}
          sortBy={sortOption}
          onSortChange={setSortOption}
          onCreateRoom={onCreateRoom}
        />
        <RoomListSkeleton count={6} />
      </div>
    );
  }

  // 에러 상태
  if (error) {
    return (
      <div>
        <RoomListHeader
          roomCount={rooms.length}
          sortBy={sortOption}
          onSortChange={setSortOption}
          onCreateRoom={onCreateRoom}
        />
        <div className="text-center py-12">
          <p className="text-semantic-error mb-4">방 목록을 불러오는 중 오류가 발생했습니다.</p>
          <p className="text-foreground-muted text-sm">{error.message}</p>
        </div>
      </div>
    );
  }

  // 빈 상태
  if (rooms.length === 0 && !isLoading) {
    return (
      <div>
        <RoomListHeader
          roomCount={0}
          sortBy={sortOption}
          onSortChange={setSortOption}
          onCreateRoom={onCreateRoom}
        />
        <RoomListEmpty onCreateRoom={onCreateRoom} />
      </div>
    );
  }

  // 정상 렌더링
  return (
    <div>
      <RoomListHeader
        roomCount={rooms.length}
        sortBy={sortOption}
        onSortChange={setSortOption}
        onCreateRoom={onCreateRoom}
      />

      <RoomListGrid rooms={rooms} onJoinRoom={onJoinRoom} />

      {/* 무한 스크롤 트리거 */}
      {hasMore && (
        <div ref={loadMoreRef} className="py-8 text-center">
          {isLoadingMore ? (
            <LoadingDots />
          ) : (
            <button
              onClick={loadMore}
              className="text-sm text-foreground-muted hover:text-foreground-primary transition-colors"
            >
              더 보기
            </button>
          )}
        </div>
      )}

      {/* 마지막 페이지 표시 */}
      {!hasMore && rooms.length > 0 && (
        <p className="text-center text-foreground-muted py-8 text-sm">
          모든 방을 불러왔습니다.
        </p>
      )}
    </div>
  );
}
