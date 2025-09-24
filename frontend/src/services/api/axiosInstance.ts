import axios from 'axios';
import { ApiErrorHandler } from './errorHandler';

const resolveBaseURL = () => {
  if (import.meta.env.DEV) {
    return 'http://localhost:8080/api';
  }

  // 프로덕션에서는 환경변수 또는 현재 호스트 사용
  const backendHost = (import.meta.env.VITE_BACKEND_HOST as string | undefined) || window.location.hostname;
  const backendPort = (import.meta.env.VITE_BACKEND_PORT as string | undefined)
    || (window.location.port === '5173' ? '8080' : window.location.port);

  const { protocol } = window.location;
  return `${protocol}//${backendHost}${backendPort ? `:${backendPort}` : ''}/api`;
};

const axiosInstance = axios.create({
  baseURL: resolveBaseURL(),
  withCredentials: true,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  }
});

axiosInstance.interceptors.request.use((config) => {
  // CSRF 토큰 처리
  const csrfToken = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content')
    || (document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/)?.[1] && decodeURIComponent(document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/)![1]));

  if (csrfToken) {
    config.headers['X-CSRF-TOKEN'] = csrfToken;
  }
// debug 용
//   console.log('  API Request Debug:');
//   console.log('  URL:', config.url);
//   console.log('  Base URL:', config.baseURL);
//   console.log('  With credentials:', config.withCredentials);
//   console.log('  All cookies:', document.cookie);

  // 세션 쿠키만 추출
  const sessionCookie = document.cookie.split('; ').find(row => row.startsWith('SESSION='));
  console.log('  Session cookie:', sessionCookie || 'NOT FOUND');
  
  // 쿠키 존재 여부 상세 확인
  if (!document.cookie) {
    console.warn('NO COOKIES FOUND AT ALL!');
  } else {
    console.log('Cookies exist:', document.cookie.split('; '));
  }
  
  // 브라우저 보안 정책 확인
  console.log('  Document domain:', document.domain);
  console.log('  Document origin:', window.location.origin);
  console.log('  Target origin:', config.baseURL);

  return config;
});

axiosInstance.interceptors.response.use(
  (response) => {
    console.log('Response headers:', response.headers);
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      console.error('401 Unauthorized:');
      console.error('Request headers:', error.config?.headers);
      console.error('Response data:', error.response?.data);

      // 쿠키 상태 확인
      const cookies = document.cookie.split('; ');
      const sessionCookie = cookies.find(cookie => cookie.startsWith('SESSION='));
      console.error('Session cookie:', sessionCookie || 'NOT FOUND');
    }

    const apiError = ApiErrorHandler.handle(error);
    return Promise.reject(apiError);
  }
);

export const checkSessionCookie = () => {
  const sessionCookie = document.cookie
    .split('; ')
    .find(row => row.startsWith('SESSION='));

  console.log('🔍 Session cookie check:', sessionCookie);
  return !!sessionCookie;
};

export default axiosInstance;