import { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import { stompClient } from '../services/ws/stompClient';
import { v4 as uuidv4 } from 'uuid';
import { getMessages } from '../services/api/messages';

interface Message {
  id: number;
  clientTempId?: string;
  roomId: number;
  user: { id: number; nickname: string; avatarUrl?: string };
  type: 'TEXT' | 'IMAGE' | 'GIF' | 'VIDEO';
  contentText?: string;
  mediaUrl?: string;
  mediaThumbUrl?: string;
  mediaDurationSec?: number;
  createdAt: string;
}

interface UseMessagesReturn {
  messages: Message[];
  isLoading: boolean;
  sendMessage: (messageData: Omit<Message, 'id' | 'clientTempId' | 'roomId' | 'user' | 'createdAt'>) => Promise<void>;
  loadMoreMessages: (roomId: number, beforeId?: number) => Promise<void>;
  hasMore: boolean;
  typingUsers: string[];
  onlineCount: number;
}

// 주의: 기존 목업 메시지를 제거하고 실제 API/WS 기반으로 동작하도록 변경

export function useMessages(roomId: number): UseMessagesReturn {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [typingUsers, setTypingUsers] = useState<string[]>([]);
  const [onlineCount, setOnlineCount] = useState(0);

  // 초기 메시지 로딩 (REST API) - 임시 비활성화
  useEffect(() => {
    // const loadInitialMessages = async () => {
    //   if (!roomId) return;
    //   setIsLoading(true);
    //   try {
    //     const data = await getMessages(roomId, undefined, 30);
    //     setMessages(data);
    //     setHasMore(data.length >= 30);
    //   } catch (e) {
    //     console.log('메시지 로딩 실패 (API-Server 연결 확인 필요):', e);
    //     // toast.error('메시지 로딩에 실패했습니다.');
    //     // 임시: 빈 메시지로 설정
    //     setMessages([]);
    //   } finally {
    //     setIsLoading(false);
    //   }
    // };
    // loadInitialMessages();

    // 임시: API 호출 없이 빈 메시지로 설정
    setMessages([]);
    setIsLoading(false);
    console.log('메시지 로딩 비활성화 - roomId:', roomId);
  }, [roomId]);

  // 메시지 전송: WS로 서버에 발행 + 낙관적 추가 - 임시 주석 처리
  const sendMessage = useCallback(async (messageData: Omit<Message, 'id' | 'clientTempId' | 'roomId' | 'user' | 'createdAt'>): Promise<void> => {
    // const clientTempId = uuidv4();
    // const optimistic: Message = {
    //   ...messageData,
    //   id: Date.now(),
    //   clientTempId,
    //   roomId,
    //   // TODO: 실제 사용자 정보는 전역 상태/세션에서 주입
    //   user: { id: 0, nickname: '나', avatarUrl: '' },
    //   createdAt: new Date().toISOString(),
    // };

    // // 낙관적 업데이트
    // setMessages(prev => [...prev, optimistic]);

    // // WS 전송 (clientTempId 포함)
    // stompClient.sendRoomMessage(roomId, {
    //   clientTempId,
    //   type: optimistic.type,
    //   contentText: optimistic.contentText,
    //   mediaUrl: optimistic.mediaUrl,
    //   mediaThumbUrl: optimistic.mediaThumbUrl,
    //   mediaDurationSec: optimistic.mediaDurationSec,
    // });

    // 임시: 아무것도 안 함
    console.log('메시지 전송 임시 비활성화:', messageData);
  }, [roomId]);

  // 과거 메시지 추가 로드 (REST API)
  const loadMoreMessages = useCallback(async (roomIdParam: number, beforeId?: number): Promise<void> => {
    // if (isLoading) return;
    // setIsLoading(true);
    // try {
    //   const older = await getMessages(roomIdParam, beforeId, 30);
    //   setMessages(prev => [...older, ...prev]);
    //   setHasMore(older.length >= 30);
    // } catch (e) {
    //   toast.error('이전 메시지를 불러오지 못했습니다.');
    // } finally {
    //   setIsLoading(false);
    // }

    // 임시: 아무것도 안 함
    console.log('과거 메시지 로딩 임시 비활성화');
  }, [isLoading]);

  // 타이핑/메시지/프레즌스 구독 설정 - 임시 주석 처리
  useEffect(() => {
    // if (!roomId) return;

    // // 메시지 구독
    // const msgSub = stompClient.subscribe(`/topic/rooms/${roomId}/messages`, (frame) => {
    //   try {
    //     const evt = JSON.parse(frame.body) as Message;
    //     setMessages((prev) => {
    //       if (evt.clientTempId) {
    //         // clientTempId 일치 시 pending → 확정으로 치환
    //         return prev.map((m) => m.clientTempId === evt.clientTempId ? { ...evt } : m);
    //       }
    //       return [...prev, evt];
    //     });
    //   } catch (e) {
    //     console.error('Failed to parse message event', e);
    //   }
    // });

    // // 타이핑 구독
    // const typingSub = stompClient.subscribe(`/topic/rooms/${roomId}/typing`, (frame) => {
    //   try {
    //     const evt = JSON.parse(frame.body) as { nickname: string; typing: boolean };
    //     setTypingUsers((prev) => {
    //       const exists = prev.includes(evt.nickname);
    //       if (evt.typing && !exists) return [...prev, evt.nickname];
    //       if (!evt.typing && exists) return prev.filter((n) => n !== evt.nickname);
    //       return prev;
    //     });
    //   } catch {}
    // });

    // // 프레즌스 구독
    // const presenceSub = stompClient.subscribe(`/topic/rooms/${roomId}/presence`, (frame) => {
    //   try {
    //     const evt = JSON.parse(frame.body) as { count: number };
    //     setOnlineCount(evt.count);
    //   } catch {}
    // });

    // return () => {
    //   if (msgSub) msgSub.unsubscribe();
    //   if (typingSub) typingSub.unsubscribe();
    //   if (presenceSub) presenceSub.unsubscribe();
    // };

    // 임시: WebSocket 구독 비활성화
    console.log('WebSocket 구독 임시 비활성화 - roomId:', roomId);
  }, [roomId]);

  // 타이핑 상태 송신 헬퍼(옵션): 외부에서 호출하도록 노출하지 않음. 컴포넌트 단에서 stompClient.sendTyping 사용 가능

  return {
    messages,
    isLoading,
    sendMessage,
    loadMoreMessages,
    hasMore,
    typingUsers,
    onlineCount,
  };
}
