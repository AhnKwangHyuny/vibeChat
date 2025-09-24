import React from 'react';
import { Provider } from 'react-redux';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { store } from '../store';
import { useSessionAuth } from '../hooks/useSessionAuth';
import { Spinner } from '../components/ui/Spinner';

const queryClient = new QueryClient();

/**
 * 앱 로드 시 인증 상태 확인 게이트키퍼
 * useSessionAuth가 모든 세션 복원 로직을 담당
 */
const AuthGate = ({ children }: { children: React.ReactNode }) => {
  const { isCheckingAuth } = useSessionAuth();
  console.log(isCheckingAuth);
  if (isCheckingAuth) {
    return (
      <div className="flex h-screen w-full items-center justify-center bg-background-primary">
        <div className="text-center">
          <Spinner size="lg" color="primary" />
          <p className="mt-4 text-foreground-muted">인증 확인 중...</p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
};

export function AppProviders({ children }: { children: React.ReactNode }) {
  return (
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>
        <AuthGate>
          {children}
        </AuthGate>
      </QueryClientProvider>
    </Provider>
  );
}
