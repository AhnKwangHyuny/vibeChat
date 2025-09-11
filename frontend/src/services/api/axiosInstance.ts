import axios from 'axios';
import { ApiErrorHandler } from './errorHandler';

// Axios 인스턴스
// - TRD에 따라 기본 Base URL은 "/api" (Nginx 프록시 전제)

const resolveBaseURL = () => {

  // 2) 별도 백엔드 호스트/포트가 주어졌으면 그것을 사용
  const backendHost = (import.meta.env.VITE_BACKEND_HOST as string | undefined) || window.location.hostname;
  const backendPort = (import.meta.env.VITE_BACKEND_PORT as string | undefined)
    // 프론트 포트가 5173면 백엔드 기본 8080로 가정(개발 기본 매핑)
    || (window.location.port === '5173' ? '8080' : window.location.port);
  
  const { protocol } = window.location;
  return `${protocol}//${backendHost}${backendPort ? `:${backendPort}` : ''}/api`;
};

const axiosInstance = axios.create({
  baseURL: resolveBaseURL(),
  withCredentials: true,
});

// CSRF 토큰을 요청 헤더에 첨부(더블 서밋)
axiosInstance.interceptors.request.use((config) => {
  // 서버가 쿠키로 설정한 CSRF 토큰 값을 메타 태그 또는 쿠키에서 읽어 헤더로 전달
  const csrfToken = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content')
    || (document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/)?.[1] && decodeURIComponent(document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/)![1]));
  if (csrfToken) {
    config.headers['X-CSRF-TOKEN'] = csrfToken;
  }
  return config;
});

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    // 중앙 에러 핸들러로 표준화(RFC7807 호환 메시지 처리)
    const apiError = ApiErrorHandler.handle(error);
    
    // For authentication errors, optionally redirect to login
    if (apiError.status === 401) {
      // Could redirect to login page or refresh token
      // window.location.href = '/login';
    }
    
    return Promise.reject(apiError);
  }
);

export default axiosInstance;
