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

let isAuthCheckInProgress = false;

export const useSessionAuth = () => {
  const dispatch = useDispatch<AppDispatch>();
  const user = useSelector((state: RootState) => state.user);
  const [authState, setAuthState] = useState<AuthState>({
    loading: false,
    error: null,
    isCheckingAuth: true,
  });

  const checkSessionAuth = useCallback(async () => {
    if (user.id || isAuthCheckInProgress) {
      setAuthState(prev => ({ ...prev, isCheckingAuth: false }));
      return;
    }

    isAuthCheckInProgress = true;
    setAuthState(prev => ({ ...prev, isCheckingAuth: true }));

    try {
      const userData = await getMe();
      if (userData) {
        dispatch(
          setUser({
            id: userData.userId.toString(),
            nickname: userData.nickname,
            avatarUrl: userData.avatarUrl,
            provider: userData.provider,
          })
        );
        return true;
      }
      return false;
    } catch (error: any) {
      if (error?.status === 401) {
        console.log('로그인되지 않은 사용자 (401)');
      } else {
        console.error('세션 확인 중 오류:', error);
      }
      return false;
    } finally {
      isAuthCheckInProgress = false;
      setAuthState(prev => ({ ...prev, isCheckingAuth: false }));
    }
  }, [dispatch, user.id]);

  useEffect(() => {
    checkSessionAuth();
  }, [checkSessionAuth]);

  const signInAsGuest = async (
    nickname: string
  ): Promise<boolean | { suggested: string }> => {
    if (!nickname.trim()) {
      toast.error('닉네임을 입력하세요');
      return false;
    }

    setAuthState(prev => ({ ...prev, loading: true, error: null }));
    try {
      const created = await createGuestUser({ nickname: nickname.trim() });
      dispatch(
        setUser({
          id: created.userId.toString(),
          nickname: created.nickname,
          provider: 'GUEST',
        })
      );
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

  const signOut = async () => {
    setAuthState(prev => ({ ...prev, loading: true, error: null }));
    try {
      await logout();
      dispatch(clearUser());
      console.log('백엔드 세션 종료 완료');
    } catch (error) {
      console.error('백엔드 로그아웃 오류:', error);
      dispatch(clearUser());
      throw error;
    } finally {
      setAuthState(prev => ({ ...prev, loading: false }));
    }
  };

  const refreshUser = async () => {
    return await checkSessionAuth();
  };

  return {
    user,
    isAuthenticated: !!user.id,
    loading: authState.loading,
    error: authState.error,
    isCheckingAuth: authState.isCheckingAuth,
    signInAsGuest,
    signOut,
    refreshUser,
    checkSessionAuth,
  };
};


