import { supabase } from './supabaseClient'
import { AuthError, Session, User } from '@supabase/supabase-js'
import api from '../api/axiosInstance'

export interface SupabaseAuthUser {
  id: string
  email?: string
  user_metadata: {
    name?: string
    avatar_url?: string
    picture?: string
    full_name?: string
  }
  app_metadata: {
    provider?: string
    providers?: string[]
  }
}

export interface BackendUserResponse {
  userId: number
  nickname: string
  avatarUrl?: string
  provider: 'GUEST' | 'GOOGLE'
}

class AuthService {
  /**
   * Google OAuth 로그인 시작
   */
  async signInWithGoogle(): Promise<{ data: any; error: AuthError | null }> {
    try {
      const { data, error } = await supabase.auth.signInWithOAuth({
        provider: 'google',
        options: {
          redirectTo: `${window.location.origin}/auth/callback`,
          queryParams: {
            access_type: 'offline',
            prompt: 'consent',
          }
        }
      })

      return { data, error }
    } catch (error) {
      console.error('Google OAuth 시작 실패:', error)
      return { data: null, error: error as AuthError }
    }
  }

  /**
   * 현재 Supabase 세션 가져오기
   */
  async getCurrentSession(): Promise<Session | null> {
    try {
      const { data: { session }, error } = await supabase.auth.getSession()
      if (error) {
        console.error('세션 가져오기 실패:', error)
        return null
      }
      return session
    } catch (error) {
      console.error('세션 가져오기 오류:', error)
      return null
    }
  }

  /**
   * 현재 Supabase 사용자 정보 가져오기
   */
  async getCurrentUser(): Promise<User | null> {
    try {
      const { data: { user }, error } = await supabase.auth.getUser()
      if (error) {
        console.error('사용자 정보 가져오기 실패:', error)
        return null
      }
      return user
    } catch (error) {
      console.error('사용자 정보 가져오기 오류:', error)
      return null
    }
  }

  /**
   * Supabase 토큰을 백엔드로 전송하여 사용자 생성/로그인 처리
   */
  async authenticateWithBackend(session: Session): Promise<BackendUserResponse> {
    try {
      const { access_token, user } = session
      
      // Supabase 사용자 정보에서 필요한 데이터 추출
      const googleUser = user as SupabaseAuthUser
      const nickname = googleUser.user_metadata.name || googleUser.user_metadata.full_name || 'Google User'
      const avatarUrl = googleUser.user_metadata.avatar_url || googleUser.user_metadata.picture
      const providerId = googleUser.id // Supabase의 고유 사용자 ID
      
      // 백엔드 API 호출
      const response = await api.post<BackendUserResponse>('/auth/google', {
        accessToken: access_token,
        providerId: providerId,
        nickname: nickname,
        avatarUrl: avatarUrl,
        email: googleUser.email
      })

      return response.data
    } catch (error) {
      console.error('백엔드 인증 실패:', error)
      throw error
    }
  }

  /**
   * 로그아웃
   */
  async signOut(): Promise<{ error: AuthError | null }> {
    try {
      // Supabase에서 로그아웃
      const { error: supabaseError } = await supabase.auth.signOut()
      
      // 백엔드 세션도 정리
      try {
        await api.post('/auth/logout')
      } catch (backendError) {
        console.error('백엔드 로그아웃 실패:', backendError)
      }

      return { error: supabaseError }
    } catch (error) {
      console.error('로그아웃 실패:', error)
      return { error: error as AuthError }
    }
  }

  /**
   * 인증 상태 변경 리스너
   */
  onAuthStateChange(callback: (event: string, session: Session | null) => void) {
    return supabase.auth.onAuthStateChange(callback)
  }

  /**
   * 액세스 토큰 새로고침
   */
  async refreshToken(): Promise<{ session: Session | null; error: AuthError | null }> {
    try {
      const { data, error } = await supabase.auth.refreshSession()
      return { session: data.session, error }
    } catch (error) {
      console.error('토큰 새로고침 실패:', error)
      return { session: null, error: error as AuthError }
    }
  }
}

export const authService = new AuthService()
export default authService
