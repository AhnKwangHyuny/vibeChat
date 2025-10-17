/**
 * ProfileView Component
 *
 * 책임: 프로필 페이지 메인 뷰 (읽기 전용)
 * - ProfileCard 표시
 * - RecentRooms 표시
 * - QuickActions 사이드바
 * - SettingsPanel 사이드바
 * - Grid 레이아웃 조합
 */

import ProfileCard from '../../../components/demo/ProfileCard';
import { RecentRooms } from './RecentRooms';
import { QuickActions } from './QuickActions';
import { SettingsPanel } from './SettingsPanel';
import { ProfileCardData } from '../types/profile';

interface ProfileViewProps {
  profile: ProfileCardData;
  recentRooms: any[]; // TODO: 향후 타입 정의
  onUpdateProfile?: (data: { nickname: string; bio: string }) => void;
  onChangeStatus?: (status: 'online' | 'away' | 'offline') => void;
}

export function ProfileView({
  profile,
  recentRooms,
  onUpdateProfile,
  onChangeStatus,
}: ProfileViewProps) {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
      {/* Main Content (3 columns) */}
      <div className="lg:col-span-3">
        {/* Profile Card */}
        <ProfileCard
          user={profile}
          onUpdateProfile={onUpdateProfile}
          onChangeStatus={onChangeStatus}
          className="mb-8"
        />

        {/* Recent Activity */}
        <RecentRooms rooms={recentRooms} />
      </div>

      {/* Sidebar (1 column) */}
      <div className="space-y-6">
        <QuickActions />
        <SettingsPanel />
      </div>
    </div>
  );
}
