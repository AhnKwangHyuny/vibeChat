import { useState, useEffect, useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { RootState, AppDispatch } from '../store';
import { setUser, clearUser } from '../store/userSlice';
import { createGuestUser, getMe, logout } from '../services/api/auth';
import { toast } from 'react-toastify';

interface AuthState {
  loading: boolean;
  error: string | null;
  isCheckingAuth: boolean;
}

export const useSessionAuth = () => {
  const dispatch = useDispatch<AppDispatch>();
  const user = useSelector((state: RootState) => state.user);
  const [authState, setAuthState] = useState<AuthState>({
    loading: false,
    error: null,
    isCheckingAuth: true
  });

  // 세션 기반 사용자 정보 확인 및 Redux 동기화
  const checkSessionAuth = useCallback(async () => {
    try {
      setAuthState(prev => ({ ...prev, isCheckingAuth: true }));
      const userData = await getMe();
      
      if (userData) {
        // Redux store에 사용자 정보 저장
        dispatch(setUser({ 
          id: userData.userId.toString(), 
          nickname: userData.nickname 
        }));
        
        console.log('세션 인증 성공:', userData);
        return true;
      }
      
      return false;
    } catch (error) {
      // 401 에러는 정상 상태 (로그인하지 않은 사용자)
      if (error instanceof Error && (error as any).response?.status === 401) {
        console.log('로그인되지 않은 사용자');
      } else {
        console.log(error)
        console.error('세션 확인 중 오류:', error);
      }
      return false;
    } finally {
      setAuthState(prev => ({ ...prev, isCheckingAuth: false }));
    }
  }, [dispatch]);

  // 초기 마운트 시 세션 확인
  useEffect(() => {
    checkSessionAuth();
  }, [checkSessionAuth]);

  // 게스트 로그인
  const signInAsGuest = async (nickname: string): Promise<boolean | { suggested: string }> => {
    if (!nickname.trim()) {
      toast.error('닉네임을 입력하세요');
      return false;
    }

    try {
      setAuthState(prev => ({ ...prev, loading: true, error: null }));
      const created = await createGuestUser({ nickname: nickname.trim() });
      
      // Redux store에 사용자 정보 저장
      dispatch(setUser({ 
        id: created.userId.toString(), 
        nickname: created.nickname 
      }));
      
      toast.success('게스트로 로그인되었습니다');
      return true;
    } catch (error: any) {
      const suggested = error?.response?.data?.suggestedNickname;
      if (suggested) {
        toast.info(`이미 사용 중입니다. 제안: ${suggested}`);
        return { suggested };
      } else {
        const message = error?.response?.data?.message || '로그인에 실패했습니다';
        toast.error(message);
        setAuthState(prev => ({ ...prev, error: message }));
        return false;
      }
    } finally {
      setAuthState(prev => ({ ...prev, loading: false }));
    }
  };

  // 로그아웃
  const signOut = async () => {
    try {
      setAuthState(prev => ({ ...prev, loading: true, error: null }));
      await logout();
      
      // Redux 상태 클리어
      dispatch(clearUser());
      
      toast.success('로그아웃되었습니다');
    } catch (error) {
      console.error('로그아웃 오류:', error);
      // 로그아웃 실패해도 로컬 상태는 클리어
      dispatch(clearUser());
      toast.error('로그아웃 중 오류가 발생했습니다');
    } finally {
      setAuthState(prev => ({ ...prev, loading: false }));
    }
  };

  // 수동으로 사용자 정보 새로고침
  const refreshUser = async () => {
    return await checkSessionAuth();
  };

  return {
    // 상태
    user,
    isAuthenticated: !!user.id,
    loading: authState.loading,
    error: authState.error,
    isCheckingAuth: authState.isCheckingAuth,
    
    // 액션
    signInAsGuest,
    signOut,
    refreshUser,
    checkSessionAuth
  };
};
