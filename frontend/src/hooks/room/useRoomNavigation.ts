import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { toast } from 'react-toastify';
import { clearUser } from '../../store/userSlice';
import { useSessionAuth } from '../useSessionAuth';
import { useSupabaseAuth } from '../useSupabaseAuth';

interface UseRoomNavigationProps {
  roomId: number;
  userProvider: 'GUEST' | 'GOOGLE' | null;
  onRoomExit?: () => Promise<void>;
}

interface UseRoomNavigationReturn {
  handleLeaveRoom: () => Promise<void>;
  handleLogin: () => void;
  handleLogout: () => Promise<void>;
  navigateToHome: () => void;
  handleFatalError: (error: Error, context: string) => void;
}

export function useRoomNavigation({
  roomId,
  userProvider,
  onRoomExit,
}: UseRoomNavigationProps): UseRoomNavigationReturn {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { signOut: sessionSignOut } = useSessionAuth();
  const { signOut: supabaseSignOut } = useSupabaseAuth();

  // 홈으로 이동
  const navigateToHome = useCallback(() => {
    navigate('/');
  }, [navigate]);

  // 방 나가기 처리
  const handleLeaveRoom = useCallback(async () => {
    console.log('[ROOM_NAVIGATION] 방 나가기 시작');

    try {
      // 외부에서 전달된 방 퇴장 로직 실행 (WebSocket 연결 해제 등)
      if (onRoomExit) {
        console.log('[ROOM_NAVIGATION] 방 퇴장 워크플로우 실행');
        await onRoomExit();
      }

      toast.success('방에서 나왔습니다.');
      navigateToHome();
    } catch (error) {
      console.error('[ROOM_NAVIGATION] 방 나가기 중 오류:', error);
      toast.error('방 나가기 중 오류가 발생했습니다.');

      // 에러가 발생해도 페이지는 이동
      navigateToHome();
    }
  }, [onRoomExit, navigateToHome]);

  // 로그인 페이지로 이동
  const handleLogin = useCallback(() => {
    console.log('[ROOM_NAVIGATION] 로그인 페이지로 이동');
    navigateToHome();
  }, [navigateToHome]);

  // 로그아웃 처리
  const handleLogout = useCallback(async () => {
    console.log('[ROOM_NAVIGATION] 로그아웃 프로세스 시작');

    try {
      // 1. 소셜 로그인 사용자의 경우 Supabase 세션 종료
      if (userProvider === 'GOOGLE') {
        await supabaseSignOut();
        console.log('[ROOM_NAVIGATION] Supabase 세션 종료 완료');
      }

      // 2. 백엔드 세션 종료 (모든 사용자 공통)
      await sessionSignOut();
      console.log('[ROOM_NAVIGATION] 백엔드 세션 종료 완료');

      toast.success('성공적으로 로그아웃되었습니다.');
      navigateToHome();
    } catch (error) {
      console.error('[ROOM_NAVIGATION] 로그아웃 실패:', error);
      toast.error('로그아웃 중 문제가 발생했습니다. 페이지를 새로고침합니다.');

      // 강제 정리 및 새로고침
      dispatch(clearUser());
      window.location.reload();
    }
  }, [userProvider, supabaseSignOut, sessionSignOut, navigateToHome, dispatch]);

  // 전역 에러 핸들러
  const handleFatalError = useCallback((error: Error, context: string) => {
    console.error(`[ROOM_NAVIGATION] 치명적 오류 - ${context}:`, error);
    toast.error(`오류가 발생했습니다: ${error.message}`);

    // 3초 후 자동으로 홈으로 리다이렉트
    setTimeout(() => {
      navigateToHome();
    }, 3000);
  }, [navigateToHome]);

  return {
    handleLeaveRoom,
    handleLogin,
    handleLogout,
    navigateToHome,
    handleFatalError,
  };
}