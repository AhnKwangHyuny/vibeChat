import React from 'react';
import { Provider } from 'react-redux';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { store } from '../store';
import { ToastContainer } from '../components/ui/ToastContainer';

const queryClient = new QueryClient();

export function AppProviders({ children }: { children: React.ReactNode }) {
  return (
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>
        {/* 전역 토스트 컨테이너 배치 (디자인 시스템 컴포넌트 사용) */}
        {children}
      </QueryClientProvider>
    </Provider>
  );
}
