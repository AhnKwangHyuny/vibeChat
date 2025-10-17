/**
 * QuickActions Component
 *
 * 책임: 빠른 작업 메뉴
 * - 방 만들기
 * - 모든 방 보기
 * - 방 검색하기
 */

import { useNavigate } from 'react-router-dom';
import { Card } from '../../../components/ui/Card';
import { Button } from '../../../components/ui/Button';

export function QuickActions() {
  const navigate = useNavigate();

  return (
    <Card>
      <div className="p-6">
        <h3 className="text-lg font-semibold text-foreground-primary mb-4">
          빠른 작업
        </h3>
        <div className="space-y-3">
          <Button
            variant="secondary"
            className="w-full justify-start"
            onClick={() => navigate('/create')}
          >
            <svg
              className="w-4 h-4 mr-2"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 4v16m8-8H4"
              />
            </svg>
            새 방 만들기
          </Button>

          <Button
            variant="secondary"
            className="w-full justify-start"
            onClick={() => navigate('/roomList')}
          >
            <svg
              className="w-4 h-4 mr-2"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"
              />
            </svg>
            모든 방 보기
          </Button>

          <Button
            variant="secondary"
            className="w-full justify-start"
            onClick={() => navigate('/')}
          >
            <svg
              className="w-4 h-4 mr-2"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
              />
            </svg>
            방 검색하기
          </Button>
        </div>
      </div>
    </Card>
  );
}
