import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSupabaseAuth } from '../hooks/useSupabaseAuth';
import { Spinner } from '../components/ui/Spinner';
import { Card } from '../components/ui/Card';
import { toast } from 'react-toastify';

const AuthCallback: React.FC = () => {
  const navigate = useNavigate();
  const { session, backendUser, loading, error } = useSupabaseAuth();
  const [processingAuth, setProcessingAuth] = useState(true);
  const [hasShownToast, setHasShownToast] = useState(false);

  useEffect(() => {
    const handleAuthCallback = async () => {
      try {
        // 인증 처리가 완료될 때까지 대기
        if (!loading) {
          if (session && backendUser) {
            // 성공적으로 인증됨 - 토스트 한 번만 표시
            console.log('인증 성공:', backendUser);
            if (!hasShownToast) {
              toast.success(`환영합니다, ${backendUser.nickname}님!`);
              setHasShownToast(true);
            }
            navigate('/', { replace: true });
          } else if (error) {
            // 인증 실패
            console.error('인증 실패:', error);
            navigate('/', { replace: true });
          } else {
            // 세션이 없는 경우 (인증되지 않음)
            navigate('/', { replace: true });
          }
          setProcessingAuth(false);
        }
      } catch (err) {
        console.error('Auth callback 처리 중 오류:', err);
        navigate('/', { replace: true });
        setProcessingAuth(false);
      }
    };

    // 약간의 지연을 두어 Supabase 인증이 완전히 처리되도록 함
    const timer = setTimeout(handleAuthCallback, 1000);

    return () => clearTimeout(timer);
  }, [session, backendUser, loading, error, navigate]);

  if (processingAuth || loading) {
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

  return (
    <div className="min-h-screen bg-background-primary flex items-center justify-center">
      <Card className="p-8 max-w-md w-full mx-4">
        <div className="text-center space-y-4">
          <div className="text-red-500">
            <h2 className="text-xl font-semibold mb-2">
              인증 처리 중 오류가 발생했습니다
            </h2>
            <p className="text-sm text-foreground-muted">
              다시 시도해주세요.
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default AuthCallback;
