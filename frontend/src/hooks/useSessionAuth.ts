import { useState, useEffect, useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { RootState, AppDispatch } from '../store';
import { setUser, clearUser } from '../store/userSlice';
import { createGuestUser, getMe, logout } from '../services/api/auth';
import { toast } from 'react-toastify';

/**
 * 세션 기반 인증을 관리하는 훅.
 * 앱 로드 시 서버에 세션 유효성을 확인하고, 그 결과를 Redux store에 반영한다.
 * 이 훅의 로직이 완료될 때까지 앱 렌더링은 AuthGate에 의해 보류된다.
 */
export const useSessionAuth = () => {
  const dispatch = useDispatch<AppDispatch>();
  const user = useSelector((state: RootState) => state.user);

  // 앱 로드 시 최초 인증 확인 중인지 여부
  const [isCheckingAuth, setIsCheckingAuth] = useState(true);
  // 개별 액션(로그인, 로그아웃 등)의 로딩 상태
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 서버에 세션 상태를 확인하고 Redux 상태를 업데이트하는 핵심 함수
  const checkSession = useCallback(async () => {
    console.log('[AUTH] Starting session check...');
    setIsCheckingAuth(true);
    try {
      const userData = await getMe();

      if (userData && userData.userId) {
        const userInfo = {
          id: userData.userId.toString(),
          nickname: userData.nickname,
          avatarUrl: userData.avatarUrl,
          provider: userData.provider,
        };
        dispatch(setUser(userInfo));
        console.log('[AUTH] Session valid. User state hydrated.', userInfo);
      } else {
        // API는 성공했지만 유저 데이터가 없는 경우 (예: 세션 만료 직후)
        console.log('[AUTH] No valid session found on server.');
        dispatch(clearUser());
      }
    } catch (err: any) {
      // API 호출 자체가 실패한 경우 (예: 401 Unauthorized, 네트워크 에러)
      console.error('[AUTH] Session check API failed. Clearing user state.', err);
      dispatch(clearUser());
    } finally {
      setIsCheckingAuth(false);
      console.log('[AUTH] Session check finished.');
    }
  }, [dispatch]);

  // 앱이 처음 마운트될 때만 세션 확인 로직을 실행
  useEffect(() => {
    checkSession();
  }, [checkSession]); // checkSession은 useCallback으로 메모이즈되어 있으므로 안전

  const signInAsGuest = async (nickname: string): Promise<boolean | { suggested: string }> => {
    setLoading(true);
    setError(null);
    try {
      const created = await createGuestUser({ nickname: nickname.trim() });
      dispatch(setUser({
        id: created.userId.toString(),
        nickname: created.nickname,
        provider: 'GUEST',
      }));
      toast.success('게스트로 로그인되었습니다');
      return true;
    } catch (err: any) {
      const suggested = err?.response?.data?.suggestedNickname;
      if (suggested) {
        toast.info(`이미 사용 중입니다. 제안: ${suggested}`);
        return { suggested };
      } else {
        const message = err?.response?.data?.message || '로그인에 실패했습니다';
        toast.error(message);
        setError(message);
        return false;
      }
    } finally {
      setLoading(false);
    }
  };

  const signOut = async (): Promise<void> => {
    setLoading(true);
    try {
      await logout();
      toast.success('로그아웃되었습니다.');
    } catch (err) {
      console.error('[AUTH] Logout API failed, forcing local cleanup.', err);
    } finally {
      dispatch(clearUser());
      setLoading(false);
    }
  };

  return {
    user,
    isAuthenticated: !!user.id,
    loading,
    error,
    isCheckingAuth,
    signInAsGuest,
    signOut,
    refreshUser: checkSession,
  };
};