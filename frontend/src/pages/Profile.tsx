import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';

// Import components from our component library
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Modal } from '../components/ui/Modal';
import { Navbar } from '../components/layout/Navbar';
import ProfileCard from '../components/demo/ProfileCard';
import RoomListCard from '../components/demo/RoomListCard';

export default function Profile() {
  const navigate = useNavigate();
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(true);
  const [user, setUser] = useState({
    id: '1',
    nickname: 'John Doe',
    email: 'john.doe@example.com',
    avatarUrl: '',
    bio: '열정적인 개발자이자 커뮤니티 빌더입니다. React와 TypeScript를 사랑하며, 좋은 UX를 만드는 것에 관심이 많습니다.',
    joinedAt: '2024-01-15T00:00:00Z',
    status: 'online' as const,
    badges: ['Early Adopter', 'Community Builder', 'Active Chatter', 'Helper'],
    stats: {
      totalMessages: 1247,
      roomsJoined: 12,
      friendsCount: 28
    }
  });

  const [recentRooms] = useState([
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
        content: '새로운 React 19 기능에 대해 어떻게 생각하세요?'
      },
      unreadCount: 3,
      isMuted: false,
      isTyping: true,
      typingUsers: ['Bob']
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
        content: '타입 추론이 너무 복잡할 때는 어떻게 하시나요?'
      },
      unreadCount: 0,
      isMuted: false
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
        content: '새로운 컬러 팔레트 제안드립니다!'
      },
      unreadCount: 1,
      isMuted: true
    }
  ]);

  const handleUpdateProfile = (data: { nickname: string; bio: string }) => {
    setUser(prev => ({ ...prev, ...data }));
    toast.success('프로필이 업데이트되었습니다!');
  };

  const handleChangeStatus = (status: 'online' | 'away' | 'offline') => {
    setUser(prev => ({ ...prev, status }));
    toast.success(`상태가 ${status === 'online' ? '온라인' : status === 'away' ? '자리비움' : '오프라인'}으로 변경되었습니다.`);
  };

  const handleDeleteAccount = () => {
    toast.success('계정이 삭제되었습니다!');
    navigate('/');
  };

  const handleLogin = () => {
    setIsLoggedIn(true);
    toast.success('로그인되었습니다!');
  };

  const handleLogout = () => {
    setIsLoggedIn(false);
    toast.success('로그아웃되었습니다!');
  };

  const handleProfileClick = () => {
    toast.info('프로필 메뉴가 클릭되었습니다!');
  };

  if (!isLoggedIn) {
    return (
      <div className="min-h-screen bg-background-primary">
        <Navbar 
          onLogin={handleLogin}
          onLogout={handleLogout}
          onProfileClick={handleProfileClick}
        />
        
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
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background-primary">
      <Navbar 
        user={user}
        onLogin={handleLogin}
        onLogout={handleLogout}
        onProfileClick={handleProfileClick}
      />
      
      <div className="max-w-6xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-8 gap-4">
          <div>
            <h1 className="text-3xl font-bold text-foreground-primary mb-2">프로필</h1>
            <p className="text-foreground-muted">계정 설정과 환경설정을 관리하세요</p>
          </div>
          <Button 
            variant="secondary" 
            onClick={() => navigate('/')}
            className="flex items-center gap-2 w-full sm:w-auto"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            홈으로 돌아가기
          </Button>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
          {/* Profile Card */}
          <div className="lg:col-span-3">
            <ProfileCard
              user={user}
              onUpdateProfile={handleUpdateProfile}
              onChangeStatus={handleChangeStatus}
              className="mb-8"
            />

            {/* Recent Activity */}
            <Card>
              <div className="p-6">
                <h3 className="text-lg font-semibold text-foreground-primary mb-4">최근 활동한 방</h3>
                <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
                  {recentRooms.map((room) => (
                    <RoomListCard
                      key={room.id}
                      room={room}
                      onJoin={() => {
                        navigate(`/room/${room.id}`);
                      }}
                      onLeave={() => {
                        toast.success(`${room.title} 방에서 나왔습니다.`);
                      }}
                      onMute={() => {
                        toast.success(`${room.title} 방의 알림을 껐습니다.`);
                      }}
                      onUnmute={() => {
                        toast.success(`${room.title} 방의 알림을 켰습니다.`);
                      }}
                    />
                  ))}
                </div>
              </div>
            </Card>
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            {/* Quick Actions */}
            <Card>
              <div className="p-6">
                <h3 className="text-lg font-semibold text-foreground-primary mb-4">빠른 작업</h3>
                <div className="space-y-3">
                  <Button
                    variant="secondary"
                    className="w-full justify-start"
                    onClick={() => navigate('/create')}
                  >
                    <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                    </svg>
                    새 방 만들기
                  </Button>
                  <Button
                    variant="secondary"
                    className="w-full justify-start"
                    onClick={() => navigate('/roomList')}
                  >
                    <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                    </svg>
                    모든 방 보기
                  </Button>
                  <Button
                    variant="secondary"
                    className="w-full justify-start"
                    onClick={() => navigate('/')}
                  >
                    <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                    방 검색하기
                  </Button>
                </div>
              </div>
            </Card>

            {/* Preferences */}
            <Card>
              <div className="p-6">
                <h3 className="text-lg font-semibold text-foreground-primary mb-4">환경설정</h3>
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-foreground-secondary">알림 받기</span>
                    <label className="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" className="sr-only peer" defaultChecked />
                      <div className="w-11 h-6 bg-background-tertiary peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-500"></div>
                    </label>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-foreground-secondary">소리 알림</span>
                    <label className="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" className="sr-only peer" />
                      <div className="w-11 h-6 bg-background-tertiary peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-500"></div>
                    </label>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-foreground-secondary">온라인 상태 표시</span>
                    <label className="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" className="sr-only peer" defaultChecked />
                      <div className="w-11 h-6 bg-background-tertiary peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-500"></div>
                    </label>
                  </div>
                </div>
              </div>
            </Card>

            {/* Danger Zone */}
            <Card className="border-semantic-error">
              <div className="p-6">
                <h3 className="text-lg font-semibold text-semantic-error mb-4">위험 구역</h3>
                <div className="space-y-3">
                  <Button 
                    onClick={() => setShowDeleteModal(true)}
                    variant="danger"
                    size="sm"
                    className="w-full"
                  >
                    계정 삭제
                  </Button>
                </div>
                <p className="text-xs text-foreground-muted mt-2">
                  계정을 삭제하면 모든 데이터가 영구적으로 삭제됩니다.
                </p>
              </div>
            </Card>
          </div>
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={showDeleteModal}
        onClose={() => setShowDeleteModal(false)}
        title="계정 삭제"
        size="md"
      >
        <div className="space-y-4">
          <p className="text-foreground-secondary">
            정말로 계정을 삭제하시겠습니까? 이 작업은 취소할 수 없습니다.
          </p>
          <div className="flex justify-end gap-2">
            <Button 
              variant="secondary" 
              onClick={() => setShowDeleteModal(false)}
            >
              취소
            </Button>
            <Button 
              variant="danger" 
              onClick={handleDeleteAccount}
            >
              계정 삭제
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}