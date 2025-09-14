/**
 * Common Types
 * 공통으로 사용되는 타입 정의
 */

// API 응답 기본 구조
export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
  timestamp: string;
}

// 페이지네이션 정보
export interface PaginationInfo {
  page: number;
  limit: number;
  total: number;
  totalPages: number;
  hasNext: boolean;
  hasPrev: boolean;
}

// 페이지네이션된 응답
export interface PaginatedResponse<T> {
  items: T[];
  pagination: PaginationInfo;
}

// 정렬 옵션
export interface SortOption {
  field: string;
  order: 'ASC' | 'DESC';
}

// 검색 옵션
export interface SearchOption {
  query?: string;
  filters?: Record<string, any>;
  sort?: SortOption;
  pagination?: {
    page: number;
    limit: number;
  };
}

// 에러 정보
export interface ErrorInfo {
  code: string;
  message: string;
  details?: Record<string, any>;
  timestamp: string;
}

// 파일 업로드 정보
export interface FileUploadInfo {
  fileName: string;
  fileSize: number;
  fileType: string;
  fileUrl: string;
  thumbnailUrl?: string;
}

// 파일 업로드 요청
export interface FileUploadRequest {
  file: File;
  type: 'IMAGE' | 'FILE' | 'AVATAR';
}

// 파일 업로드 응답
export interface FileUploadResponse {
  success: boolean;
  fileInfo?: FileUploadInfo;
  error?: string;
}

// 웹소켓 메시지 타입
export type WebSocketMessageType = 
  | 'MESSAGE_SENT'
  | 'MESSAGE_UPDATED'
  | 'MESSAGE_DELETED'
  | 'USER_JOINED'
  | 'USER_LEFT'
  | 'TYPING_START'
  | 'TYPING_STOP'
  | 'ROOM_UPDATED'
  | 'ERROR';

// 웹소켓 메시지
export interface WebSocketMessage<T = any> {
  type: WebSocketMessageType;
  data: T;
  timestamp: string;
  roomId?: number;
  userId?: string;
}

// 알림 타입
export type NotificationType = 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR';

// 알림 정보
export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
  actionUrl?: string;
}

// 설정 정보
export interface AppSettings {
  theme: 'light' | 'dark' | 'auto';
  language: 'ko' | 'en';
  notifications: {
    sound: boolean;
    desktop: boolean;
    email: boolean;
  };
  privacy: {
    showOnlineStatus: boolean;
    allowDirectMessages: boolean;
  };
}

// 통계 정보
export interface Statistics {
  totalUsers: number;
  totalRooms: number;
  totalMessages: number;
  activeUsers: number;
  onlineUsers: number;
}
