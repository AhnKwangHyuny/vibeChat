import { useState, useEffect } from 'react'
import { Session, User } from '@supabase/supabase-js'
import { authService, BackendUserResponse } from '../services/supabase/authService'
import { toast } from 'react-toastify'
import { useDispatch } from 'react-redux'
import { AppDispatch } from '../store'
import { setUser } from '../store/userSlice'

interface AuthState {
  session: Session | null
  user: User | null
  backendUser: BackendUserResponse | null
  loading: boolean
  error: string | null
}

export const useSupabaseAuth = () => {
  const dispatch = useDispatch<AppDispatch>()
  const [authState, setAuthState] = useState<AuthState>({
    session: null,
    user: null,
    backendUser: null,
    loading: true,
    error: null
  })
  const [isAuthenticating, setIsAuthenticating] = useState(false)

  // Google OAuth 로그인
  const signInWithGoogle = async () => {
    try {
      setAuthState(prev => ({ ...prev, loading: true, error: null }))
      
      const { error } = await authService.signInWithGoogle()
      
      if (error) {
        setAuthState(prev => ({ ...prev, error: error.message, loading: false }))
        toast.error(`로그인 실패: ${error.message}`)
        return false
      }
      
      // OAuth 리다이렉트가 발생하므로 여기서는 로딩 상태 유지
      return true
    } catch (error) {
      const message = error instanceof Error ? error.message : '알 수 없는 오류'
      setAuthState(prev => ({ ...prev, error: message, loading: false }))
      toast.error(`로그인 오류: ${message}`)
      return false
    }
  }

  // 로그아웃
  const signOut = async () => {
    try {
      setAuthState(prev => ({ ...prev, loading: true }))
      
      const { error } = await authService.signOut()
      
      if (error) {
        toast.error(`로그아웃 실패: ${error.message}`)
      } else {
        toast.success('로그아웃되었습니다')
      }
      
      setAuthState({
        session: null,
        user: null,
        backendUser: null,
        loading: false,
        error: null
      })
    } catch (error) {
      const message = error instanceof Error ? error.message : '알 수 없는 오류'
      setAuthState(prev => ({ ...prev, error: message, loading: false }))
      toast.error(`로그아웃 오류: ${message}`)
    }
  }

  // 백엔드 인증 처리
  const authenticateWithBackend = async (session: Session) => {
    // 이미 인증 중이면 중복 실행 방지
    if (isAuthenticating) return null
    
    try {
      setIsAuthenticating(true)
      const backendUser = await authService.authenticateWithBackend(session)
      setAuthState(prev => ({ 
        ...prev, 
        backendUser, 
        loading: false,
        error: null 
      }))
      
      // Redux store에 사용자 정보 저장
      if (backendUser) {
        dispatch(setUser({ 
          id: backendUser.userId.toString(), 
          nickname: backendUser.nickname, 
          avatarUrl: backendUser.avatarUrl 
        }))
        console.log('Redux에 사용자 정보 저장:', backendUser)
      }
      
      // 토스트는 AuthCallback에서 표시
      return backendUser
    } catch (error) {
      const message = error instanceof Error ? error.message : '백엔드 인증 실패'
      setAuthState(prev => ({ ...prev, error: message, loading: false }))
      // 토스트는 ApiErrorHandler에서 이미 표시하므로 중복 제거
      return null
    } finally {
      setIsAuthenticating(false)
    }
  }

  // 초기 세션 확인 및 인증 상태 리스너 설정
  useEffect(() => {
    // 인증 상태 변경 리스너
    const { data: { subscription } } = authService.onAuthStateChange(
      async (event, session) => {

        if (event === 'INITIAL_SESSION' && session) {
            setAuthState(prev => ({ ...prev, session, user: session.user, loading: true }));
            await authenticateWithBackend(session);
        } else if (event === 'SIGNED_IN' && session) {
          setAuthState(prev => ({
            ...prev,
            session,
            user: session.user,
            loading: true
          }))

          // 백엔드 인증 처리
          await authenticateWithBackend(session)

        } else if (event === 'SIGNED_OUT') {
          setAuthState({
            session: null,
            user: null,
            backendUser: null,
            loading: false,
            error: null
          })
        } else if (event === 'TOKEN_REFRESHED' && session) {
          setAuthState(prev => ({
            ...prev,
            session,
            user: session.user
          }))
        } else if (event === 'USER_UPDATED' && session) {
            setAuthState(prev => ({ ...prev, user: session.user }));
        }
      }
    )

    return () => subscription.unsubscribe()
  }, [])

  return {
    // 상태
    session: authState.session,
    user: authState.user,
    backendUser: authState.backendUser,
    loading: authState.loading,
    error: authState.error,
    isAuthenticated: !!authState.session && !!authState.backendUser,
    
    // 액션
    signInWithGoogle,
    signOut,
    
    // 유틸리티
    refreshToken: authService.refreshToken
  }
}
