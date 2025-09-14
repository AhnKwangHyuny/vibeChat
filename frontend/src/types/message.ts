/**
 * Message Domain Types
 * 메시지(Message) 관련 도메인 모델 정의
 */

// 메시지 타입
export type MessageType = 'TEXT' | 'IMAGE' | 'FILE' | 'VOICE' | 'SYSTEM';

// 메시지 상태
export type MessageStatus = 'SENT' | 'DELIVERED' | 'READ' | 'FAILED';

// 메시지 정보
export interface Message {
  id: number;
  roomId: number;
  userId: string;
  userNickname: string;
  userAvatarUrl?: string;
  content: string;
  type: MessageType;
  status: MessageStatus;
  createdAt: string;
  updatedAt: string;
  // 첨부 파일 정보
  attachments?: MessageAttachment[];
  // 답장 정보
  replyTo?: MessageReply;
  // 반응 정보
  reactions?: MessageReaction[];
}

// 메시지 첨부 파일
export interface MessageAttachment {
  id: number;
  fileName: string;
  fileSize: number;
  fileType: string;
  fileUrl: string;
  thumbnailUrl?: string;
}

// 메시지 답장
export interface MessageReply {
  messageId: number;
  userNickname: string;
  content: string;
  type: MessageType;
}

// 메시지 반응
export interface MessageReaction {
  emoji: string;
  count: number;
  users: string[]; // 사용자 ID 목록
}

// 메시지 전송 요청
export interface SendMessageRequest {
  roomId: number;
  content: string;
  type: MessageType;
  replyTo?: number; // 답장할 메시지 ID
  attachments?: File[];
}

// 메시지 전송 응답
export interface SendMessageResponse {
  success: boolean;
  message?: Message;
  error?: string;
}

// 메시지 목록 조회 요청
export interface GetMessagesRequest {
  roomId: number;
  limit?: number;
  offset?: number;
  before?: string; // 특정 시간 이전 메시지
  after?: string; // 특정 시간 이후 메시지
}

// 메시지 목록 조회 응답
export interface GetMessagesResponse {
  messages: Message[];
  hasMore: boolean;
  totalCount: number;
}

// 메시지 삭제 요청
export interface DeleteMessageRequest {
  messageId: number;
}

// 메시지 삭제 응답
export interface DeleteMessageResponse {
  success: boolean;
  message: string;
}

// 메시지 수정 요청
export interface EditMessageRequest {
  messageId: number;
  content: string;
}

// 메시지 수정 응답
export interface EditMessageResponse {
  success: boolean;
  message?: Message;
  error?: string;
}

// 메시지 반응 추가 요청
export interface AddReactionRequest {
  messageId: number;
  emoji: string;
}

// 메시지 반응 추가 응답
export interface AddReactionResponse {
  success: boolean;
  message: string;
}

// 메시지 반응 제거 요청
export interface RemoveReactionRequest {
  messageId: number;
  emoji: string;
}

// 메시지 반응 제거 응답
export interface RemoveReactionResponse {
  success: boolean;
  message: string;
}

// 타이핑 상태
export interface TypingStatus {
  userId: string;
  userNickname: string;
  roomId: number;
  isTyping: boolean;
  timestamp: string;
}

// 타이핑 상태 업데이트 요청
export interface UpdateTypingStatusRequest {
  roomId: number;
  isTyping: boolean;
}

// 타이핑 상태 업데이트 응답
export interface UpdateTypingStatusResponse {
  success: boolean;
  message: string;
}
