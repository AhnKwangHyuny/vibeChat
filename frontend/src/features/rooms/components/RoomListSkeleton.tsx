/**
 * RoomListSkeleton Component
 * 방 목록 로딩 스켈레톤 UI
 * Home.tsx 346-362줄의 기존 디자인 유지
 */

import { Card } from '../../../components/ui/Card';

interface RoomListSkeletonProps {
  count?: number;
}

export function RoomListSkeleton({ count = 6 }: RoomListSkeletonProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {[...Array(count)].map((_, i) => (
        <Card key={i} className="p-6">
          <div className="animate-pulse">
            <div className="h-6 bg-background-tertiary rounded mb-3"></div>
            <div className="h-4 bg-background-tertiary rounded mb-2"></div>
            <div className="h-4 bg-background-tertiary rounded mb-4 w-3/4"></div>
            <div className="flex gap-2 mb-4">
              <div className="h-6 bg-background-tertiary rounded w-16"></div>
              <div className="h-6 bg-background-tertiary rounded w-20"></div>
            </div>
            <div className="h-10 bg-background-tertiary rounded"></div>
          </div>
        </Card>
      ))}
    </div>
  );
}
