import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSupabaseAuth } from '../hooks/useSupabaseAuth';
import { Spinner } from '../components/ui/Spinner';
import { Card } from '../components/ui/Card';
import { toast } from 'react-toastify';

const AuthCallback: React.FC = () => {
  const navigate = useNavigate();
  // useSupabaseAuth 훅의 로딩 상태를 직접 사용
  const { session, backendUser, loading, error } = useSupabaseAuth();
  const [hasShownToast, setHasShownToast] = useState(false);

  useEffect(() => {
    // 로딩이 끝나면 처리 시작
    if (!loading) {
      if (session && backendUser) {
        // 성공적으로 인증됨 - 토스트 한 번만 표시
        if (!hasShownToast) {
          toast.success(`환영합니다, ${backendUser.nickname}님!`);
          setHasShownToast(true);
        }
        navigate('/', { replace: true });
      } else {
        // 인증 실패 또는 세션 없음
        if (error) {
          console.error('인증 실패:', error);
        }
        navigate('/', { replace: true });
      }
    }
  }, [session, backendUser, loading, error, navigate, hasShownToast]);

  // useSupabaseAuth의 로딩 상태를 그대로 UI에 반영
  if (loading) {
    return (
      <div className="min-h-screen bg-background-primary flex items-center justify-center">
        <Card className="p-8 max-w-md w-full mx-4">
          <div className="text-center space-y-4">
            <Spinner size="lg" color="primary" />
            <div>
              <h2 className="text-xl font-semibold text-foreground-primary mb-2">
                로그인 처리 중...
              </h2>
              <p className="text-sm text-foreground-muted">
                Google 계정으로 로그인하고 있습니다. 잠시만 기다려주세요.
              </p>
            </div>
          </div>
        </Card>
      </div>
    );
  }

  // 로딩이 끝났지만 인증에 실패한 경우 (예: 에러 발생)
  return (
    <div className="min-h-screen bg-background-primary flex items-center justify-center">
      <Card className="p-8 max-w-md w-full mx-4">
        <div className="text-center space-y-4">
          <div className="text-red-500">
            <h2 className="text-xl font-semibold mb-2">
              인증 처리 중 오류가 발생했습니다
            </h2>
            <p className="text-sm text-foreground-muted">
              홈으로 이동하여 다시 시도해주세요.
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default AuthCallback;
