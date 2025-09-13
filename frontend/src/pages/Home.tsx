import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useNavigate, useLocation } from 'react-router-dom';
import { toast } from 'react-toastify';

// Import components from our component library
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Tooltip } from '../components/ui/Tooltip';
import { RoomCard } from '../components/demo/RoomCard';
import { TagInput } from '../components/demo/TagInput';
import { Hero } from '../components/layout/Hero';
import { Navbar } from '../components/layout/Navbar';
import NicknameModal from '../features/user/NicknameModal';
import { Footer } from '../components/layout/Footer';
import SearchBar from '../components/ui/SearchBar';
import NotificationBadge from '../components/ui/NotificationBadge';
import LoadingDots from '../components/ui/LoadingDots';
import { useSessionAuth } from '../hooks/useSessionAuth';
import { useSupabaseAuth } from '../hooks/useSupabaseAuth';
import { clearUser } from '../store/userSlice';
import { useDispatch } from 'react-redux';

const searchSchema = z.object({
  tags: z.array(z.string()).min(1, "Please enter at least one tag"),
});

type SearchFormInputs = z.infer<typeof searchSchema>;

export default function Home() {
  const navigate = useNavigate();
  const location = useLocation();
  const isLegacy = new URLSearchParams(location.search).get('legacy') === '1';
  const { handleSubmit, setValue, watch, formState: { errors } } = useForm<SearchFormInputs>({
    resolver: zodResolver(searchSchema),
  });

  const [searchQuery, setSearchQuery] = useState<string[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [showLogin, setShowLogin] = useState(false);
  const [sortBy, setSortBy] = useState<'newest' | 'oldest' | 'participants'>('newest');
  
  const dispatch = useDispatch();

  // 인증 훅 사용
  const { user, isAuthenticated, signOut: sessionSignOut } = useSessionAuth();
  const { signOut: supabaseSignOut } = useSupabaseAuth();
  // Mock data for development
  const mockRooms = [
    {
      id: 1,
      title: "React Developers",
      description: "Discuss React and modern web development",
      tags: ["react", "javascript", "frontend"],
      participantsCount: 15,
      isPrivate: false,
      lastMessageAt: new Date().toISOString()
    },
    {
      id: 2,
      title: "TypeScript Enthusiasts",
      description: "TypeScript tips and tricks",
      tags: ["typescript", "javascript", "programming"],
      participantsCount: 8,
      isPrivate: false,
      lastMessageAt: new Date(Date.now() - 3600000).toISOString()
    },
    {
      id: 3,
      title: "Design Systems",
      description: "Building consistent UI components",
      tags: ["design", "ui", "components"],
      participantsCount: 12,
      isPrivate: true,
      lastMessageAt: new Date(Date.now() - 7200000).toISOString()
    },
    {
      id: 4,
      title: "VibeChat Community",
      description: "General discussion about VibeChat platform",
      tags: ["community", "general", "chat"],
      participantsCount: 25,
      isPrivate: false,
      lastMessageAt: new Date(Date.now() - 1800000).toISOString()
    },
    {
      id: 5,
      title: "Web Development",
      description: "Full-stack web development discussions",
      tags: ["web", "development", "fullstack"],
      participantsCount: 18,
      isPrivate: false,
      lastMessageAt: new Date(Date.now() - 900000).toISOString()
    }
  ];

  const onSubmit = async (data: SearchFormInputs) => {
    setIsSearching(true);
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1000));
    setSearchQuery(data.tags);
    setIsSearching(false);
  };

  const handleJoinRoom = async (roomId: number, isPrivate: boolean) => {
    // Mock join room functionality
      if (isPrivate) {
        const code = prompt("This is a private room. Please enter the invite code:");
        if (!code) return;
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 500));
      } else {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 500));
      }
      toast.success("Successfully joined room!");
      navigate(`/rooms/${roomId}`);
  };

  const handleCreateRoom = () => {
    navigate('/create');
  };

  const handleLogin = () => {
    setShowLogin(true);
  };

  const handleLogout = async () => {
    console.log("로그아웃을 시작합니다...");
    try {
      if (user.provider === 'GOOGLE') {
        await supabaseSignOut();
        console.log("Supabase 세션이 종료되었습니다.");
      }

      // 2. (모든 사용자 공통) VibeChat 백엔드 세션 종료
      await sessionSignOut();
      console.log("VibeChat 백엔드 세션이 종료되었습니다.");

      // 3. 성공 알림 (Redux의 clearUser는 각 signOut 훅에서 이미 처리하고 있음)
      toast.success("성공적으로 로그아웃되었습니다.");

    } catch (error) {
      console.error("로그아웃 중 오류 발생:", error);
      toast.error("로그아웃 중 문제가 발생했습니다. 페이지를 새로고침합니다.");

      // 최악의 경우에도 UI를 초기화하고 새로고침하여 상태를 완전히 정리
      dispatch(clearUser());
      window.location.reload();
    }
  };

  const currentTags = watch('tags') || [];
  const rooms = searchQuery.length > 0 ? mockRooms : [];
  const isLoadingRooms = isSearching;

  // Sort rooms based on selected criteria
  const sortedRooms = [...rooms].sort((a, b) => {
    switch (sortBy) {
      case 'newest':
        return new Date(b.lastMessageAt || 0).getTime() - new Date(a.lastMessageAt || 0).getTime();
      case 'oldest':
        return new Date(a.lastMessageAt || 0).getTime() - new Date(b.lastMessageAt || 0).getTime();
      case 'participants':
        return b.participantsCount - a.participantsCount;
      default:
        return 0;
    }
  });

  return (
    <div className="min-h-screen bg-background-primary">
      <Navbar
        user={isAuthenticated && user.id ? { id: user.id, nickname: user.nickname || '' } : undefined}
        onLogin={handleLogin}
        onLogout={handleLogout}
      />
      <NicknameModal isOpen={showLogin} onClose={() => setShowLogin(false)} />
      
      <main>
        {isLegacy ? (
          <>
            {/* Hero Section */}
            <Hero
              title="당신의 커뮤니티를 찾아보세요"
              subtitle="같은 관심사를 가진 사람들과 연결하세요"
              description="실시간 채팅을 통해 같은 관심사를 가진 사람들과 연결하세요. 관심사에 따라 방을 발견하고 의미 있는 대화를 시작하세요."
              primaryAction={{
                label: "채팅 시작하기",
                onClick: () => document.getElementById('search-section')?.scrollIntoView({ behavior: 'smooth' })
              }}
              secondaryAction={{
                label: "방 만들기",
                onClick: handleCreateRoom
              }}
              badges={[
                "실시간 채팅",
                "태그 기반 발견", 
                "안전하고 비공개"
              ]}
              stats={[
                { value: "150+", label: "활성 방" },
                { value: "2.5K+", label: "일일 사용자" },
                { value: "50K+", label: "전송된 메시지" }
              ]}
            />

            {/* Search Section */}
            <section id="search-section" className="py-16 bg-background-secondary">
              <div className="container mx-auto px-4">
                <div className="max-w-4xl mx-auto">
                  <div className="text-center mb-12">
                    <h2 className="text-3xl font-bold text-foreground-primary mb-4">
                      채팅방 발견하기
                    </h2>
                    <p className="text-lg text-foreground-muted">
                      태그로 방을 검색하고 관심 있는 대화에 참여하세요
                    </p>
                  </div>

                  <Card className="p-8">
                    {/* Enhanced Search Bar */}
                    <div className="space-y-6">
                      <div>
                        <label className="block text-sm font-medium text-foreground-primary mb-3">
                          방 검색
                        </label>
                        <SearchBar
                          placeholder="방 제목, 태그 또는 설명으로 검색..."
                          onSearch={(query) => {
                            if (query.trim()) {
                              setIsSearching(true);
                              // Simulate search based on query
                              const searchTerms = query.toLowerCase().split(' ');
                              const filteredRooms = mockRooms.filter(room => 
                                searchTerms.some(term =>
                                  room.title.toLowerCase().includes(term) ||
                                  room.description?.toLowerCase().includes(term) ||
                                  room.tags.some(tag => tag.toLowerCase().includes(term))
                                )
                              );
                              setTimeout(() => {
                                setSearchQuery(searchTerms);
                                setIsSearching(false);
                                if (filteredRooms.length === 0) {
                                  toast.info('검색 결과가 없습니다. 다른 키워드로 시도해보세요.');
                                }
                              }, 1000);
                            }
                          }}
                          suggestions={['react', 'typescript', 'design', 'web development', 'javascript', 'frontend', 'backend']}
                          showFilters={true}
                          filters={[
                            { id: 'public', label: '공개방', active: true },
                            { id: 'private', label: '비공개방', active: false },
                            { id: 'active', label: '활성 방', active: true }
                          ]}
                          onFilterClick={() => {
                            toast.info('필터 기능은 곧 제공될 예정입니다!');
                          }}
                        />
                        {errors.tags && (
                          <p className="text-sm text-semantic-error mt-2">{errors.tags.message}</p>
                        )}
                      </div>

                      {/* Alternative Tag Input for specific searches */}
                      <div>
                        <label className="block text-sm font-medium text-foreground-primary mb-3">
                          또는 태그로 정확히 검색
                        </label>
                        <form onSubmit={handleSubmit(onSubmit)}>
                          <TagInput
                            tags={currentTags}
                            onTagsChange={(tags) => setValue('tags', tags)}
                            placeholder="정확한 태그를 입력하세요..."
                            maxTags={5}
                          />
                          <div className="flex justify-center mt-4">
                            <Button
                              type="submit"
                              variant="secondary"
                              size="md"
                              disabled={isSearching}
                              className="px-6"
                            >
                              {isSearching ? (
                                <>
                                  <LoadingDots size="sm" className="mr-2" />
                                  검색 중
                                </>
                              ) : (
                                '태그로 검색'
                              )}
                            </Button>
                          </div>
                        </form>
                      </div>
                    </div>
                  </Card>
                </div>
              </div>
            </section>

            {/* Results Section */}
            {searchQuery.length > 0 && (
              <section className="py-16 bg-background-primary">
                <div className="container mx-auto px-4">
                  <div className="max-w-6xl mx-auto">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-6 sm:mb-8 gap-4">
                      <div>
                        <h3 className="text-xl sm:text-2xl font-bold text-foreground-primary mb-2">
                          사용 가능한 방
                        </h3>
                        <p className="text-sm sm:text-base text-foreground-muted">
                          검색 결과 {sortedRooms.length}개의 방을 찾았습니다
                        </p>
                      </div>
                      <div className="flex flex-col sm:flex-row gap-3 sm:gap-4">
                        <div className="relative">
                          <select
                            value={sortBy}
                            onChange={(e) => setSortBy(e.target.value as 'newest' | 'oldest' | 'participants')}
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
                        <Button variant="secondary" onClick={handleCreateRoom} className="flex items-center gap-2">
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                          </svg>
                          새 방 만들기
                        </Button>
                      </div>
                    </div>

                    {isLoadingRooms ? (
                      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {[...Array(6)].map((_, i) => (
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
                    ) : rooms.length === 0 ? (
                      <Card className="p-12 text-center">
                        <div className="text-6xl mb-4">🔍</div>
                        <h4 className="text-xl font-semibold text-foreground-primary mb-2">No rooms found</h4>
                        <p className="text-foreground-muted mb-6">Try different tags or create a new room for this topic</p>
                        <Button onClick={handleCreateRoom}>Create New Room</Button>
                      </Card>
                    ) : (
                      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {sortedRooms.map((room) => (
                          <NotificationBadge key={room.id} count={Math.floor(Math.random() * 5) + 1} variant="danger" position="top-right">
                            <RoomCard
                              id={room.id.toString()}
                              title={room.title}
                              description={room.description}
                              tags={room.tags}
                              isPrivate={room.isPrivate}
                              participantsCount={room.participantsCount}
                              lastMessageAt={room.lastMessageAt}
                              onJoin={() => handleJoinRoom(room.id, room.isPrivate)}
                            />
                          </NotificationBadge>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </section>
            )}

            {/* Popular Tags Section */}
            {searchQuery.length === 0 && (
              <section className="py-16 bg-background-secondary">
                <div className="container mx-auto px-4">
                  <div className="max-w-6xl mx-auto text-center">
                    <h3 className="text-2xl font-bold text-foreground-primary mb-8">인기 주제</h3>
                    <div className="relative overflow-hidden mb-8">
                      <div className="flex animate-scroll">
                        <div className="flex gap-3 whitespace-nowrap">
                          {['react', 'typescript', 'javascript', 'design', 'ui', 'programming', 'web', 'development'].map((tag) => (
                            <Tooltip key={`first-${tag}`} content={`${tag} 방 검색하기`}>
                              <Button variant="secondary" size="sm" onClick={() => { setValue('tags', [tag]); setSearchQuery([tag]); }} className="rounded-full flex-shrink-0">#{tag}</Button>
                            </Tooltip>
                          ))}
                        </div>
                        <div className="flex gap-3 whitespace-nowrap ml-6">
                          {['react', 'typescript', 'javascript', 'design', 'ui', 'programming', 'web', 'development'].map((tag) => (
                            <Tooltip key={`second-${tag}`} content={`${tag} 방 검색하기`}>
                              <Button variant="secondary" size="sm" onClick={() => { setValue('tags', [tag]); setSearchQuery([tag]); }} className="rounded-full flex-shrink-0">#{tag}</Button>
                            </Tooltip>
                          ))}
                        </div>
                      </div>
                    </div>
                    <p className="text-foreground-muted">태그를 클릭하여 관련 방을 검색하세요</p>
                  </div>
                </div>
              </section>
            )}

            {/* Features Section */}
            <section className="py-16 bg-background-primary">
              <div className="container mx-auto px-4">
                <div className="max-w-6xl mx-auto">
                  <div className="text-center mb-8 sm:mb-12 px-4">
                    <h3 className="text-xl sm:text-2xl md:text-3xl font-bold text-foreground-primary mb-3 sm:mb-4">VibeChat을 선택하는 이유</h3>
                    <p className="text-sm sm:text-base md:text-lg text-foreground-muted max-w-2xl mx-auto">실시간 커뮤니케이션의 미래를 경험하세요</p>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6 lg:gap-8">
                    <Card className="p-4 sm:p-6 text-center">
                      <div className="w-10 h-10 sm:w-12 sm:h-12 bg-semantic-info/10 rounded-lg flex items-center justify-center mx-auto mb-3 sm:mb-4">
                        <svg className="w-5 h-5 sm:w-6 sm:h-6 text-semantic-info" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" /></svg>
                      </div>
                      <h4 className="text-lg sm:text-xl font-semibold text-foreground-primary mb-2">실시간 메시징</h4>
                      <p className="text-sm sm:text-base text-foreground-muted">WebSocket 기술로 즉시 메시지를 전송하여 번개처럼 빠른 커뮤니케이션을 경험하세요</p>
                    </Card>
                    <Card className="p-4 sm:p-6 text-center">
                      <div className="w-10 h-10 sm:w-12 sm:h-12 bg-semantic-success/10 rounded-lg flex items-center justify-center mx-auto mb-3 sm:mb-4">
                        <svg className="w-5 h-5 sm:w-6 sm:h-6 text-semantic-success" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" /></svg>
                      </div>
                      <h4 className="text-lg sm:text-xl font-semibold text-foreground-primary mb-2">태그 기반 발견</h4>
                      <p className="text-sm sm:text-base text-foreground-muted">지능적인 태그 시스템을 사용하여 관심사에 맞는 방을 찾아보세요</p>
                    </Card>
                    <Card className="p-4 sm:p-6 text-center">
                      <div className="w-10 h-10 sm:w-12 sm:h-12 bg-semantic-warning/10 rounded-lg flex items-center justify-center mx-auto mb-3 sm:mb-4">
                        <svg className="w-5 h-5 sm:w-6 sm:h-6 text-semantic-warning" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" /></svg>
                      </div>
                      <h4 className="text-lg sm:text-xl font-semibold text-foreground-primary mb-2">안전하고 비공개</h4>
                      <p className="text-sm sm:text-base text-foreground-muted">종단간 암호화와 개인정보 보호 기능으로 대화를 보호합니다</p>
                    </Card>
                  </div>
                </div>
              </div>
            </section>
          </>
        ) : (
          <>
            {/* Compact Search Bar Section */}
            <section className="py-6 bg-background-secondary border-b border-border-default">
              <div className="container mx-auto px-4">
                <div className="max-w-4xl mx-auto">
                  <SearchBar
                    placeholder="방 제목, 태그 또는 설명으로 검색..."
                    onSearch={(query) => {
                      const terms = query.toLowerCase().split(' ').filter(Boolean);
                      setSearchQuery(terms);
                    }}
                    suggestions={['react','typescript','javascript','design','ui','web','chat','music','ai']}
                    showFilters={true}
                    filters={[
                      { id: 'public', label: '공개', active: true },
                      { id: 'private', label: '비공개', active: false },
                      { id: 'live', label: '라이브', active: true }
                    ]}
                    onFilterClick={() => toast.info('필터 기능은 곧 제공될 예정입니다!')}
                  />
                </div>
              </div>
            </section>

            {/* Trending Tags Bar */}
            <section className="py-3 bg-background-primary">
              <div className="container mx-auto px-4">
                <div className="flex gap-2 overflow-x-auto no-scrollbar">
                  {['react','typescript','design','music','game','ai','photo','travel','movie','startup'].map(tag => (
                    <Button key={tag} variant="secondary" size="sm" className="rounded-full flex-shrink-0" onClick={() => { setValue('tags',[tag]); setSearchQuery([tag]); }}>
                      #{tag}
                    </Button>
                  ))}
                </div>
              </div>
            </section>

            {/* Quick Actions */}
            <section className="py-4 bg-background-primary">
              <div className="container mx-auto px-4">
                <div className="grid grid-cols-3 gap-3 sm:gap-4">
                  <Button variant="primary" onClick={handleCreateRoom}>방 만들기</Button>
                  <Button variant="secondary" onClick={() => {
                    const pool = mockRooms.filter(r => !r.isPrivate);
                    const r = pool[Math.floor(Math.random()*pool.length)];
                    if (r) handleJoinRoom(r.id, r.isPrivate);
                  }}>랜덤 입장</Button>
                  <Button variant="ghost" onClick={async () => {
                    const code = prompt('초대 코드를 입력하세요:');
                    if (!code) return;
                    await new Promise(res => setTimeout(res, 500));
                    toast.success('초대 코드 확인됨 (데모)');
                  }}>초대코드</Button>
                </div>
              </div>
            </section>

            {/* Live Rooms Feed */}
            <section className="py-8 bg-background-primary">
              <div className="container mx-auto px-4">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-xl font-bold text-foreground-primary">실시간 방</h3>
                  <div className="relative">
                    <select
                      value={sortBy}
                      onChange={(e) => setSortBy(e.target.value as 'newest' | 'oldest' | 'participants')}
                      className="appearance-none bg-background-secondary border border-border-default rounded-base px-3 py-2 pr-8 text-sm text-foreground-primary"
                    >
                      <option value="newest">최신순</option>
                      <option value="participants">인원수순</option>
                      <option value="oldest">오래된순</option>
                    </select>
                    <div className="absolute inset-y-0 right-0 flex items-center pr-2 pointer-events-none">
                      <svg className="w-4 h-4 text-foreground-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" /></svg>
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {mockRooms.map((room) => (
                    <NotificationBadge key={room.id} count={Math.floor(Math.random()*8)} variant="danger" position="top-right">
                      <RoomCard
                        id={room.id.toString()}
                        title={room.title}
                        description={room.description}
                        tags={room.tags}
                        isPrivate={room.isPrivate}
                        participantsCount={room.participantsCount}
                        lastMessageAt={room.lastMessageAt}
                        onJoin={() => handleJoinRoom(room.id, room.isPrivate)}
                      />
                    </NotificationBadge>
                  ))}
                </div>
              </div>
            </section>
          </>
        )}
      </main>

      <Footer />
    </div>
  );
}