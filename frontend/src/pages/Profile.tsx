import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';

import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Navbar } from '../components/layout/Navbar';
import { Footer } from '../components/layout/Footer';
import { useSessionAuth } from '../hooks/useSessionAuth';
import { useSupabaseAuth } from '../hooks/useSupabaseAuth';
import { clearUser } from '../store/userSlice';
import { useDispatch } from 'react-redux';
import NicknameModal from '../features/user/NicknameModal';

// Profile Feature
import { useProfile } from '../features/profile/hooks/useProfile';
import { toProfileCardData } from '../features/profile/types/profile';
import { ProfileView } from '../features/profile/components/ProfileView';

// Mock data for recent rooms (TODO: 향후 API 연동)
const MOCK_RECENT_ROOMS = [
  {
    id: 1,
    title: 'React Developers',
    description: 'React와 관련된 모든 것을 이야기하는 공간입니다.',
    tags: ['react', 'javascript', 'frontend'],
    participantsCount: 15,
    isPrivate: false,
    lastMessageAt: new Date(Date.now() - 300000).toISOString(),
    lastMessage: {
      user: 'Alice',
      content: '새로운 React 19 기능에 대해 어떻게 생각하세요?',
    },
    unreadCount: 3,
    isMuted: false,
    isTyping: true,
    typingUsers: ['Bob'],
  },
  {
    id: 2,
    title: 'TypeScript Enthusiasts',
    description: 'TypeScript 팁과 트릭을 공유하는 곳',
    tags: ['typescript', 'javascript', 'programming'],
    participantsCount: 8,
    isPrivate: false,
    lastMessageAt: new Date(Date.now() - 1800000).toISOString(),
    lastMessage: {
      user: 'Charlie',
      content: '타입 추론이 너무 복잡할 때는 어떻게 하시나요?',
    },
    unreadCount: 0,
    isMuted: false,
  },
  {
    id: 3,
    title: 'Design Systems',
    description: '일관된 UI 컴포넌트 구축하기',
    tags: ['design', 'ui', 'components'],
    participantsCount: 12,
    isPrivate: true,
    lastMessageAt: new Date(Date.now() - 3600000).toISOString(),
    lastMessage: {
      user: 'Diana',
      content: '새로운 컬러 팔레트 제안드립니다!',
    },
    unreadCount: 1,
    isMuted: true,
  },
];

export default function Profile() {
  const navigate = useNavigate();
  const [showLogin, setShowLogin] = useState(false);
  const dispatch = useDispatch();

  // 인증 훅
  const { user, isAuthenticated, signOut: sessionSignOut } = useSessionAuth();
  const { signOut: supabaseSignOut } = useSupabaseAuth();

  // 프로필 데이터 조회 (API)
  const { data: profileResponse, isLoading, isError, error } = useProfile();

  const handleLogin = () => {
    setShowLogin(true);
  };

  const handleLogout = async () => {
    console.log('로그아웃을 시작합니다...');
    try {
      if (user.provider === 'GOOGLE') {
        await supabaseSignOut();
        console.log('Supabase 세션이 종료되었습니다.');
      }

      await sessionSignOut();
      console.log('VibeChat 백엔드 세션이 종료되었습니다.');

      toast.success('성공적으로 로그아웃되었습니다.');
    } catch (error) {
      console.error('로그아웃 중 오류 발생:', error);
      toast.error('로그아웃 중 문제가 발생했습니다. 페이지를 새로고침합니다.');

      dispatch(clearUser());
      window.location.reload();
    }
  };

  const handleProfileClick = () => {
    toast.info('프로필 메뉴가 클릭되었습니다!');
  };

  const handleUpdateProfile = (data: { nickname: string; bio: string }) => {
    // TODO: API 연동 (PATCH /api/profile/me)
    toast.success('프로필이 업데이트되었습니다!');
  };

  const handleChangeStatus = (status: 'online' | 'away' | 'offline') => {
    // TODO: Redis 상태 업데이트 API 연동
    toast.success(
      `상태가 ${
        status === 'online'
          ? '온라인'
          : status === 'away'
          ? '자리비움'
          : '오프라인'
      }으로 변경되었습니다.`
    );
  };

  // 로딩 상태
  if (isLoading) {
    return (
      <div className="min-h-screen bg-background-primary">
        <Navbar
          user={
            isAuthenticated && user.id
              ? { id: user.id, nickname: user.nickname || '' }
              : undefined
          }
          onLogin={handleLogin}
          onLogout={handleLogout}
          onProfileClick={handleProfileClick}
        />
        <NicknameModal isOpen={showLogin} onClose={() => setShowLogin(false)} />

        <div className="flex items-center justify-center min-h-[80vh]">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-500 mx-auto mb-4"></div>
            <p className="text-foreground-muted">프로필을 불러오는 중...</p>
          </div>
        </div>

        <Footer />
      </div>
    );
  }

  // 에러 상태
  if (isError) {
    return (
      <div className="min-h-screen bg-background-primary">
        <Navbar
          user={
            isAuthenticated && user.id
              ? { id: user.id, nickname: user.nickname || '' }
              : undefined
          }
          onLogin={handleLogin}
          onLogout={handleLogout}
          onProfileClick={handleProfileClick}
        />
        <NicknameModal isOpen={showLogin} onClose={() => setShowLogin(false)} />

        <div className="flex items-center justify-center min-h-[80vh] px-4">
          <Card className="p-8 text-center max-w-md mx-auto">
            <h2 className="text-2xl font-bold text-semantic-error mb-4">
              프로필을 불러올 수 없습니다
            </h2>
            <p className="text-foreground-muted mb-6">
              {error?.message || '알 수 없는 오류가 발생했습니다.'}
            </p>
            <Button onClick={() => navigate('/')} variant="primary" size="lg">
              홈으로 돌아가기
            </Button>
          </Card>
        </div>

        <Footer />
      </div>
    );
  }

  // 프로필 데이터가 없는 경우 (비정상)
  if (!profileResponse) {
    return (
      <div className="min-h-screen bg-background-primary">
        <Navbar
          user={
            isAuthenticated && user.id
              ? { id: user.id, nickname: user.nickname || '' }
              : undefined
          }
          onLogin={handleLogin}
          onLogout={handleLogout}
          onProfileClick={handleProfileClick}
        />
        <NicknameModal isOpen={showLogin} onClose={() => setShowLogin(false)} />

        <div className="flex items-center justify-center min-h-[80vh] px-4">
          <Card className="p-8 text-center max-w-md mx-auto">
            <h2 className="text-2xl font-bold text-foreground-primary mb-4">
              로그인이 필요합니다
            </h2>
            <p className="text-foreground-muted mb-6">
              프로필을 보려면 로그인해야 합니다.
            </p>
            <Button onClick={handleLogin} variant="primary" size="lg">
              로그인
            </Button>
          </Card>
        </div>

        <Footer />
      </div>
    );
  }

  // API 응답 → UI 데이터 변환
  const profileCardData = toProfileCardData(profileResponse);

  return (
    <div className="min-h-screen bg-background-primary">
      <Navbar
        user={
          isAuthenticated && user.id
            ? { id: user.id, nickname: user.nickname || '' }
            : undefined
        }
        onLogin={handleLogin}
        onLogout={handleLogout}
        onProfileClick={handleProfileClick}
      />
      <NicknameModal isOpen={showLogin} onClose={() => setShowLogin(false)} />

      <div className="max-w-6xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-8 gap-4">
          <div>
            <h1 className="text-3xl font-bold text-foreground-primary mb-2">
              프로필
            </h1>
            <p className="text-foreground-muted">
              계정 설정과 환경설정을 관리하세요
            </p>
          </div>
          <Button
            variant="secondary"
            onClick={() => navigate('/')}
            className="flex items-center gap-2 w-full sm:w-auto"
          >
            <svg
              className="w-4 h-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M10 19l-7-7m0 0l7-7m-7 7h18"
              />
            </svg>
            홈으로 돌아가기
          </Button>
        </div>

        {/* Profile View */}
        <ProfileView
          profile={profileCardData}
          recentRooms={MOCK_RECENT_ROOMS}
          onUpdateProfile={handleUpdateProfile}
          onChangeStatus={handleChangeStatus}
        />
      </div>

      <Footer />
    </div>
  );
}
