import { useState, useEffect } from 'react';
import { toast } from 'react-toastify';

interface User {
  id: string;
  nickname: string;
  avatarUrl?: string;
  email?: string;
  joinDate?: string;
  totalMessages?: number;
  roomsCreated?: number;
  roomsJoined?: number;
}

interface AuthState {
  isLoggedIn: boolean;
  user: User;
  isLoading: boolean;
}

export function useAuth() {
  const [authState, setAuthState] = useState<AuthState>({
    isLoggedIn: false,
    user: {
      id: '1',
      nickname: 'Guest User',
      avatarUrl: '',
    },
    isLoading: true,
  });

  // Initialize auth state from localStorage
  useEffect(() => {
    const savedAuth = localStorage.getItem('vibechat_auth');
    if (savedAuth) {
      try {
        const parsedAuth = JSON.parse(savedAuth);
        setAuthState(parsedAuth);
      } catch (error) {
        console.error('Failed to parse saved auth state:', error);
        localStorage.removeItem('vibechat_auth');
      }
    }
    setAuthState(prev => ({ ...prev, isLoading: false }));
  }, []);

  // Save auth state to localStorage
  useEffect(() => {
    if (!authState.isLoading) {
      localStorage.setItem('vibechat_auth', JSON.stringify(authState));
    }
  }, [authState]);

  const login = async (userData: Partial<User>) => {
    setAuthState(prev => ({
      ...prev,
      isLoggedIn: true,
      user: {
        ...prev.user,
        ...userData,
      },
    }));
    toast.success('Successfully logged in!');
  };

  const logout = () => {
    setAuthState({
      isLoggedIn: false,
      user: {
        id: '1',
        nickname: 'Guest User',
        avatarUrl: '',
      },
      isLoading: false,
    });
    toast.success('Successfully logged out!');
  };

  const updateUser = (userData: Partial<User>) => {
    setAuthState(prev => ({
      ...prev,
      user: {
        ...prev.user,
        ...userData,
      },
    }));
  };

  const isGuest = !authState.isLoggedIn;
  const isAuthenticated = authState.isLoggedIn;

  return {
    ...authState,
    login,
    logout,
    updateUser,
    isGuest,
    isAuthenticated,
  };
}
