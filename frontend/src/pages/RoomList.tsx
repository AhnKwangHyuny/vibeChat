import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';

// Import components from our component library
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Navbar } from '../components/layout/Navbar';
import SearchBar from '../components/ui/SearchBar';
import RoomListCard from '../components/demo/RoomListCard';
import { cn } from '../utils/cn';

export default function RoomList() {
  const navigate = useNavigate();
  const [isLoggedIn, setIsLoggedIn] = useState(true);
  const [user, setUser] = useState({ 
    id: '1', 
    nickname: 'John Doe', 
    avatarUrl: '' 
  });

  const [activeTab, setActiveTab] = useState<'joined' | 'created' | 'bookmarked'>('joined');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState<'recent' | 'unread' | 'name'>('recent');

  // Mock data for different room categories
  const joinedRooms = [
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
    },
    {
      id: 4,
      title: 'VibeChat Community',
      description: 'VibeChat 플랫폼에 대한 일반 토론',
      tags: ['community', 'general', 'chat'],
      participantsCount: 25,
      isPrivate: false,
      lastMessageAt: new Date(Date.now() - 7200000).toISOString(),
      lastMessage: {
        user: 'Eve',
        content: '새로운 기능 정말 좋네요!'
      },
      unreadCount: 5,
      isMuted: false
    },
    {
      id: 5,
      title: 'Web Development',
      description: '풀스택 웹 개발 토론',
      tags: ['web', 'development', 'fullstack'],
      participantsCount: 18,
      isPrivate: false,
      lastMessageAt: new Date(Date.now() - 10800000).toISOString(),
      lastMessage: {
        user: 'Frank',
        content: 'Next.js 13에서 App Router 사용해보신 분 있나요?'
      },
      unreadCount: 0,
      isMuted: false
    }
  ];

  const createdRooms = [
    {
      id: 6,
      title: 'My Study Group',
      description: '개인 스터디 그룹입니다.',
      tags: ['study', 'personal', 'learning'],
      participantsCount: 4,
      isPrivate: true,
      lastMessageAt: new Date(Date.now() - 1200000).toISOString(),
      lastMessage: {
        user: 'John Doe',
        content: '오늘 스터디 자료 공유드립니다.'
      },
      unreadCount: 0,
      isMuted: false
    },
    {
      id: 7,
      title: 'Project Alpha Team',
      description: '알파 프로젝트 팀 논의',
      tags: ['project', 'team', 'work'],
      participantsCount: 6,
      isPrivate: true,
      lastMessageAt: new Date(Date.now() - 900000).toISOString(),
      lastMessage: {
        user: 'Grace',
        content: '다음 주 미팅 시간 조정이 필요합니다.'
      },
      unreadCount: 2,
      isMuted: false
    }
  ];

  const bookmarkedRooms = [
    {
      id: 8,
      title: 'Tech News & Updates',
      description: '최신 기술 뉴스와 업데이트',
      tags: ['tech', 'news', 'updates'],
      participantsCount: 50,
      isPrivate: false,
      lastMessageAt: new Date(Date.now() - 600000).toISOString(),
      lastMessage: {
        user: 'TechBot',
        content: 'OpenAI가 새로운 모델을 발표했습니다!'
      },
      unreadCount: 8,
      isMuted: false
    }
  ];

  const getCurrentRooms = () => {
    switch (activeTab) {
      case 'joined':
        return joinedRooms;
      case 'created':
        return createdRooms;
      case 'bookmarked':
        return bookmarkedRooms;
      default:
        return joinedRooms;
    }
  };

  const filteredRooms = getCurrentRooms().filter(room => {
    if (!searchQuery) return true;
    const query = searchQuery.toLowerCase();
    return (
      room.title.toLowerCase().includes(query) ||
      room.description?.toLowerCase().includes(query) ||
      room.tags.some(tag => tag.toLowerCase().includes(query))
    );
  });

  const sortedRooms = [...filteredRooms].sort((a, b) => {
    switch (sortBy) {
      case 'recent':
        return new Date(b.lastMessageAt).getTime() - new Date(a.lastMessageAt).getTime();
      case 'unread':
        return b.unreadCount - a.unreadCount;
      case 'name':
        return a.title.localeCompare(b.title);
      default:
        return 0;
    }
  });

  const totalUnreadCount = joinedRooms.reduce((sum, room) => sum + room.unreadCount, 0);

  const handleLogin = () => {
    setIsLoggedIn(true);
    toast.success('로그인되었습니다!');
  };

  const handleLogout = () => {
    setIsLoggedIn(false);
    toast.success('로그아웃되었습니다!');
  };

  const handleProfileClick = () => {
    navigate('/profile');
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
              방 목록을 보려면 로그인해야 합니다.
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
            <h1 className="text-3xl font-bold text-foreground-primary mb-2">내가 들어간 방</h1>
            <p className="text-foreground-muted">
              참여 중인 모든 방을 확인하고 관리하세요
              {totalUnreadCount > 0 && (
                <span className="ml-2 inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-semantic-error text-white">
                  {totalUnreadCount}개의 새 메시지
                </span>
              )}
            </p>
          </div>
          <div className="flex gap-2">
            <Button 
              variant="secondary" 
              onClick={() => navigate('/')}
              className="flex items-center gap-2"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              방 찾기
            </Button>
            <Button 
              variant="primary" 
              onClick={() => navigate('/create')}
              className="flex items-center gap-2"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
              </svg>
              새 방 만들기
            </Button>
          </div>
        </div>

        {/* Controls */}
        <div className="flex flex-col lg:flex-row gap-4 mb-6">
          {/* Tabs */}
          <div className="flex bg-background-secondary rounded-lg p-1">
            <button
              onClick={() => setActiveTab('joined')}
              className={cn(
                "px-4 py-2 text-sm font-medium rounded-md transition-colors",
                activeTab === 'joined'
                  ? "bg-background-primary text-foreground-primary shadow-sm"
                  : "text-foreground-muted hover:text-foreground-primary"
              )}
            >
              참여 중인 방 ({joinedRooms.length})
            </button>
            <button
              onClick={() => setActiveTab('created')}
              className={cn(
                "px-4 py-2 text-sm font-medium rounded-md transition-colors",
                activeTab === 'created'
                  ? "bg-background-primary text-foreground-primary shadow-sm"
                  : "text-foreground-muted hover:text-foreground-primary"
              )}
            >
              내가 만든 방 ({createdRooms.length})
            </button>
            <button
              onClick={() => setActiveTab('bookmarked')}
              className={cn(
                "px-4 py-2 text-sm font-medium rounded-md transition-colors",
                activeTab === 'bookmarked'
                  ? "bg-background-primary text-foreground-primary shadow-sm"
                  : "text-foreground-muted hover:text-foreground-primary"
              )}
            >
              북마크 ({bookmarkedRooms.length})
            </button>
          </div>

          {/* Search and Sort */}
          <div className="flex flex-col sm:flex-row gap-3 flex-1">
            <div className="flex-1">
              <SearchBar
                placeholder="방 제목이나 태그로 검색..."
                onSearch={setSearchQuery}
                suggestions={['react', 'typescript', 'design', 'community']}
                value={searchQuery}
              />
            </div>
            <div className="flex gap-2">
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as any)}
                className="px-3 py-2 bg-background-secondary border border-border-default rounded-md text-foreground-primary text-sm"
              >
                <option value="recent">최신 활동순</option>
                <option value="unread">읽지 않은 메시지순</option>
                <option value="name">이름순</option>
              </select>
            </div>
          </div>
        </div>

        {/* Room List */}
        {sortedRooms.length === 0 ? (
          <Card className="p-12 text-center">
            <div className="text-6xl mb-4">📭</div>
            <h3 className="text-xl font-semibold text-foreground-primary mb-2">
              {searchQuery ? '검색 결과가 없습니다' : '방이 없습니다'}
            </h3>
            <p className="text-foreground-muted mb-6">
              {searchQuery 
                ? '다른 검색어로 시도해보세요.'
                : activeTab === 'joined' 
                  ? '아직 참여한 방이 없습니다. 새로운 방을 찾아보거나 만들어보세요!'
                  : activeTab === 'created'
                    ? '아직 만든 방이 없습니다. 새로운 방을 만들어보세요!'
                    : '아직 북마크한 방이 없습니다.'
              }
            </p>
            {!searchQuery && (
              <div className="flex flex-col sm:flex-row gap-3 justify-center">
                <Button onClick={() => navigate('/')} variant="primary">
                  방 찾아보기
                </Button>
                <Button onClick={() => navigate('/create')} variant="secondary">
                  새 방 만들기
                </Button>
              </div>
            )}
          </Card>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 items-stretch">
            {sortedRooms.map((room) => (
              <RoomListCard
                key={room.id}
                room={room}
                onJoin={() => {
                  navigate(`/room/${room.id}`);
                }}
                onLeave={() => {
                  toast.success(`${room.title} 방에서 나왔습니다.`);
                  // In a real app, this would update the room list
                }}
                onMute={() => {
                  toast.success(`${room.title} 방의 알림을 껐습니다.`);
                  // In a real app, this would update the room's mute status
                }}
                onUnmute={() => {
                  toast.success(`${room.title} 방의 알림을 켰습니다.`);
                  // In a real app, this would update the room's mute status
                }}
              />
            ))}
          </div>
        )}

        {/* Summary Stats */}
        {sortedRooms.length > 0 && (
          <div className="mt-8 grid grid-cols-2 lg:grid-cols-4 gap-4">
            <Card className="p-4 text-center">
              <div className="text-2xl font-bold text-foreground-primary mb-1">
                {joinedRooms.length}
              </div>
              <div className="text-sm text-foreground-muted">참여 중인 방</div>
            </Card>
            <Card className="p-4 text-center">
              <div className="text-2xl font-bold text-foreground-primary mb-1">
                {createdRooms.length}
              </div>
              <div className="text-sm text-foreground-muted">내가 만든 방</div>
            </Card>
            <Card className="p-4 text-center">
              <div className="text-2xl font-bold text-foreground-primary mb-1">
                {totalUnreadCount}
              </div>
              <div className="text-sm text-foreground-muted">읽지 않은 메시지</div>
            </Card>
            <Card className="p-4 text-center">
              <div className="text-2xl font-bold text-foreground-primary mb-1">
                {bookmarkedRooms.length}
              </div>
              <div className="text-sm text-foreground-muted">북마크한 방</div>
            </Card>
          </div>
        )}
      </div>
    </div>
  );
}
