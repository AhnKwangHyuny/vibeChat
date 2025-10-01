import { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import { stompClient } from '../services/ws/stompClient';
import { v4 as uuidv4 } from 'uuid';
import { getMessages } from '../services/api/messages';
import { useSelector } from 'react-redux';
import { RootState } from '../store';

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

  // Redux에서 사용자 정보 가져오기
  const user = useSelector((state: RootState) => state.user);

  // 초기 메시지 로딩 (REST API from API-Server)
  useEffect(() => {
    const loadInitialMessages = async () => {
      if (!roomId || roomId <= 0) {
        console.log('[USE_MESSAGES] Skipping message load - invalid roomId:', roomId);
        return;
      }
      setIsLoading(true);
      try {
        // TODO: [2025-09-22] 백엔드 메시지 조회 API 구현 후 주석 해제
        console.log('Skipping initial message load: API not implemented yet.');
        setMessages([]); // 메시지 목록을 빈 배열로 초기화
        setHasMore(false); // 더 불러올 메시지가 없는 것으로 설정
        
        // const data = await getMessages(roomId, undefined, 30);
        // setMessages(data);
        // setHasMore(data.length >= 30);
        // console.log('Messages loaded successfully:', data.length);
      } catch (e) {
        console.error('Failed to load messages from API-Server:', e);
        toast.error('메시지 로딩에 실패했습니다.');
        setMessages([]);
      } finally {
        setIsLoading(false);
      }
    };
    loadInitialMessages();
  }, [roomId]);

  // 메시지 전송: WS로 Chat-Server에 발행 + 낙관적 UI 업데이트
  const sendMessage = useCallback(async (messageData: Omit<Message, 'id' | 'clientTempId' | 'roomId' | 'user' | 'createdAt'>): Promise<void> => {
    console.log('🔍 [3] useMessages sendMessage 호출:', {
      messageData: messageData,
      userId: user.id,
      nickname: user.nickname,
      roomId: roomId
    });

    if (!user.id || !user.nickname) {
      console.log('🔍 [3] 사용자 인증 실패:', {
        userId: user.id,
        nickname: user.nickname
      });
      toast.error('사용자 인증이 필요합니다.');
      return;
    }

    console.log('🔍 [3] 사용자 인증 성공, 메시지 생성 시작');

    const clientTempId = uuidv4();
    const optimistic: Message = {
      ...messageData,
      id: Date.now(), // 임시 ID
      clientTempId,
      roomId,
      user: { id: parseInt(user.id), nickname: user.nickname, avatarUrl: user.avatarUrl || undefined },
      createdAt: new Date().toISOString(),
    };

    // 낙관적 UI 업데이트
    setMessages(prev => [...prev, optimistic]);

    try {
      // WebSocket으로 Chat-Server에 메시지 전송
      console.log('🔍 [3] stompClient.sendRoomMessage 호출 시작:', {
        roomId: roomId,
        clientTempId: clientTempId,
        payload: {
          clientTempId,
          type: messageData.type,
          contentText: messageData.contentText,
          mediaUrl: messageData.mediaUrl,
          mediaThumbUrl: messageData.mediaThumbUrl,
          mediaDurationSec: messageData.mediaDurationSec,
        }
      });

      await stompClient.sendRoomMessage(roomId, {
        clientTempId,
        type: messageData.type,
        contentText: messageData.contentText,
        mediaUrl: messageData.mediaUrl,
        mediaThumbUrl: messageData.mediaThumbUrl,
        mediaDurationSec: messageData.mediaDurationSec,
      });

      console.log('🔍 [3] stompClient.sendRoomMessage 호출 완료:', { roomId, clientTempId });
    } catch (error) {
      console.error('Failed to send message:', error);
      // 실패 시 낙관적 업데이트 롤백
      setMessages(prev => prev.filter(m => m.clientTempId !== clientTempId));
      toast.error('메시지 전송에 실패했습니다.');
    }
  }, [roomId, user]);

  // 과거 메시지 추가 로드 (REST API from API-Server)
  const loadMoreMessages = useCallback(async (roomIdParam: number, beforeId?: number): Promise<void> => {
    if (isLoading) return;
    setIsLoading(true);
    try {
      console.log('Loading more messages for room:', roomIdParam, 'before:', beforeId);
      const older = await getMessages(roomIdParam, beforeId, 30);
      setMessages(prev => [...older, ...prev]);
      setHasMore(older.length >= 30);
      console.log('Loaded more messages:', older.length);
    } catch (e) {
      console.error('Failed to load more messages:', e);
      toast.error('이전 메시지를 불러오지 못했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, [isLoading]);

  // WebSocket 구독 설정 (메시지, 타이핑, 프레즌스)
  useEffect(() => {
    if (!roomId || roomId <= 0 || !user.id) {
      console.log('WebSocket 구독 스킵 - roomId 또는 user.id 없음:', { roomId, userId: user.id });
      return;
    }

    console.log('Setting up WebSocket subscriptions for room:', roomId);

    const subscriptions: Array<{ unsubscribe: () => void }> = [];

    try {
      // 메시지 구독
      const msgSub = stompClient.subscribe(`/topic/rooms/${roomId}/messages`, (frame) => {
        try {
          const evt = JSON.parse(frame.body) as Message;
          console.log('Received message via WebSocket:', evt);

          setMessages((prev) => {
            if (evt.clientTempId) {
              // clientTempId 일치 시 낙관적 업데이트를 서버 응답으로 치환
              const hasOptimistic = prev.some(m => m.clientTempId === evt.clientTempId);
              if (hasOptimistic) {
                return prev.map((m) => m.clientTempId === evt.clientTempId ? { ...evt } : m);
              }
            }
            // 새 메시지 추가 (중복 확인)
            const exists = prev.some(m => m.id === evt.id);
            if (!exists) {
              return [...prev, evt];
            }
            return prev;
          });
        } catch (e) {
          console.error('Failed to parse message event:', e);
        }
      });
      if (msgSub) subscriptions.push(msgSub);

      // 타이핑 구독
      const typingSub = stompClient.subscribe(`/topic/rooms/${roomId}/typing`, (frame) => {
        try {
          const evt = JSON.parse(frame.body) as { nickname: string; typing: boolean };
          console.log('Received typing event:', evt);

          setTypingUsers((prev) => {
            const exists = prev.includes(evt.nickname);
            if (evt.typing && !exists) return [...prev, evt.nickname];
            if (!evt.typing && exists) return prev.filter((n) => n !== evt.nickname);
            return prev;
          });
        } catch (e) {
          console.error('Failed to parse typing event:', e);
        }
      });
      if (typingSub) subscriptions.push(typingSub);

      // 프레즌스 구독
      const presenceSub = stompClient.subscribe(`/topic/rooms/${roomId}/presence`, (frame) => {
        try {
          const evt = JSON.parse(frame.body) as { count: number };
          console.log('Received presence event:', evt);
          setOnlineCount(evt.count);
        } catch (e) {
          console.error('Failed to parse presence event:', e);
        }
      });
      if (presenceSub) subscriptions.push(presenceSub);

      console.log('WebSocket subscriptions established for room:', roomId);
    } catch (error) {
      console.error('Failed to set up WebSocket subscriptions:', error);
    }

    return () => {
      console.log('Cleaning up WebSocket subscriptions for room:', roomId);
      subscriptions.forEach(sub => {
        try {
          if (sub && typeof sub.unsubscribe === 'function') {
            sub.unsubscribe();
          }
        } catch (e) {
          console.error('Error unsubscribing:', e);
        }
      });
    };
  }, [roomId, user.id]);

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
