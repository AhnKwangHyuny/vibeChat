import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';

import { Button } from '../components/ui/Button';
import { Navbar } from '../components/layout/Navbar';
import NicknameModal from '../features/user/NicknameModal';
import { Footer } from '../components/layout/Footer';
import SearchBar from '../components/ui/SearchBar';
import { useSessionAuth } from '../hooks/useSessionAuth';
import { useSupabaseAuth } from '../hooks/useSupabaseAuth';
import { clearUser } from '../store/userSlice';
import { useDispatch } from 'react-redux';
import { RoomList } from '../features/rooms/components/RoomList';

export default function Home() {
  const navigate = useNavigate();
  const [showLogin, setShowLogin] = useState(false);
  const dispatch = useDispatch();

  // 인증 훅
  const { user, isAuthenticated, signOut: sessionSignOut } = useSessionAuth();
  const { signOut: supabaseSignOut } = useSupabaseAuth();

  const handleJoinRoom = async (roomId: number, isPrivate: boolean) => {

      if (isPrivate) {
        const code = prompt("비공개 방입니다. 초대 코드를 입력해 주세요");
        if (!code) return;

      await new Promise(resolve => setTimeout(resolve, 500));
      } else {

      await new Promise(resolve => setTimeout(resolve, 500));
      }
      toast.success("채팅방에 성공적으로 입장하셨습니다!");
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

  return (
    <div className="min-h-screen flex flex-col bg-background-primary">
      <Navbar
        user={isAuthenticated && user.id ? { id: user.id, nickname: user.nickname || '' } : undefined}
        onLogin={handleLogin}
        onLogout={handleLogout}
      />
      <NicknameModal isOpen={showLogin} onClose={() => setShowLogin(false)} />

      <main className="flex-1 overflow-hidden flex flex-col">
        {/* Compact Search Bar Section */}
        <section className="py-6 bg-background-secondary border-b border-border-default">
          <div className="container mx-auto px-4">
            <div className="max-w-4xl mx-auto">
              <SearchBar
                placeholder="방 제목, 태그 또는 설명으로 검색..."
                onSearch={() => {
                  toast.info('검색 기능은 곧 제공될 예정입니다!');
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
                <Button
                  key={tag}
                  variant="secondary"
                  size="sm"
                  className="rounded-full flex-shrink-0"
                  onClick={() => toast.info('태그 검색 기능은 곧 제공될 예정입니다!')}
                >
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
                toast.info('랜덤 입장 기능은 곧 제공될 예정입니다!');
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

        {/* Live Rooms Feed - Scrollable Area */}
        <section className="flex-1 overflow-auto py-8 bg-background-primary">
          <div className="container mx-auto px-4">
            <h3 className="text-xl font-bold text-foreground-primary mb-4">실시간 방</h3>
            <RoomList onJoinRoom={handleJoinRoom} onCreateRoom={handleCreateRoom} />
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
}
