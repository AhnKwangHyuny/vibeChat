import { useState, useEffect, useCallback } from 'react';
import { toast } from 'react-toastify';
import { stompClient } from '../../services/ws/stompClient';

interface UseRoomConnectionProps {
  roomId: number;
  userId: string | null;
  nickname: string | null;
  avatarUrl?: string | null;
  isAuthenticated: boolean;
}

interface UseRoomConnectionReturn {
  isWebSocketConnected: boolean;
  connectionError: string | null;
  reconnect: () => Promise<void>;
  disconnect: () => Promise<void>;
}

export function useRoomConnection({
  roomId,
  userId,
  nickname,
  avatarUrl,
  isAuthenticated,
}: UseRoomConnectionProps): UseRoomConnectionReturn {
  const [isWebSocketConnected, setIsWebSocketConnected] = useState(false);
  const [connectionError, setConnectionError] = useState<string | null>(null);

  // stompClient 연결 상태를 주기적으로 동기화
  useEffect(() => {
    const syncConnectionStatus = () => {
      const actualConnectionStatus = stompClient.connected;
      if (actualConnectionStatus !== isWebSocketConnected) {
        console.log('[ROOM_CONNECTION] 연결 상태 동기화:', {
          previous: isWebSocketConnected,
          actual: actualConnectionStatus
        });
        setIsWebSocketConnected(actualConnectionStatus);
      }
    };

    // 초기 동기화
    syncConnectionStatus();

    // 500ms마다 연결 상태 체크
    const interval = setInterval(syncConnectionStatus, 500);

    return () => clearInterval(interval);
  }, [isWebSocketConnected]);

  // WebSocket 연결 함수
  const connectToRoom = useCallback(async () => {
    if (!isAuthenticated || !roomId || !userId || !nickname) {
      console.log('[ROOM_CONNECTION] 연결 조건 불만족:', {
        isAuthenticated,
        roomId,
        userId,
        nickname,
      });
      return;
    }

    try {
      console.log('[ROOM_CONNECTION] 방 입장 워크플로우 시작:', {
        roomId,
        userId,
        nickname,
      });

      setConnectionError(null);

      // 통합 방 입장 워크플로우 실행
      await stompClient.enterRoom(roomId, userId, nickname, avatarUrl || undefined);

      // 연결 상태는 syncConnectionStatus에서 자동 동기화됨
      toast.success('실시간 채팅에 연결되었습니다.');

      console.log('[ROOM_CONNECTION] 방 입장 워크플로우 완료');
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '연결 실패';
      console.error('[ROOM_CONNECTION] 방 입장 워크플로우 실패:', error);

      setConnectionError(errorMessage);
      // 연결 상태는 syncConnectionStatus에서 자동 동기화됨
      toast.error('방 입장에 실패했습니다.');
    }
  }, [isAuthenticated, roomId, userId, nickname, avatarUrl]);

  // WebSocket 연결 해제 함수
  const disconnectFromRoom = useCallback(async () => {
    if (!roomId) {
      console.log('[ROOM_CONNECTION] 연결 해제: roomId 없음');
      return;
    }

    try {
      console.log('[ROOM_CONNECTION] 방 퇴장 워크플로우 시작:', { roomId });

      // 방 퇴장 워크플로우 (타임아웃 10초)
      await Promise.race([
        stompClient.exitRoom(roomId),
        new Promise((_, reject) =>
          setTimeout(() => reject(new Error('Exit timeout')), 10000)
        ),
      ]);

      console.log('[ROOM_CONNECTION] 방 퇴장 워크플로우 완료');
    } catch (error) {
      console.error('[ROOM_CONNECTION] 방 퇴장 워크플로우 실패:', error);
    } finally {
      // 에러가 발생해도 로컬 상태는 정리
      // 연결 상태는 syncConnectionStatus에서 자동 동기화됨
      setConnectionError(null);
    }
  }, [roomId]);

  // 재연결 함수
  const reconnect = useCallback(async () => {
    console.log('[ROOM_CONNECTION] 재연결 시도');
    await disconnectFromRoom();
    await connectToRoom();
  }, [disconnectFromRoom, connectToRoom]);

  // 수동 연결 해제 함수 (외부 호출용)
  const disconnect = useCallback(async () => {
    console.log('[ROOM_CONNECTION] 수동 연결 해제');
    await disconnectFromRoom();
  }, [disconnectFromRoom]);

  // 방 입장 효과
  useEffect(() => {
    // 연결 조건 재확인
    if (!isAuthenticated || !roomId || !userId || !nickname) {
      console.log('[ROOM_CONNECTION] 연결 조건 불만족, 스킵:', {
        isAuthenticated,
        roomId,
        userId,
        nickname,
      });
      return;
    }

    connectToRoom();

    // 컴포넌트 언마운트 시 연결 해제
    return () => {
      if (roomId) {
        console.log('[ROOM_CONNECTION] useEffect cleanup - 방 퇴장 시작');
        // 언마운트 시에는 직접 호출로 순환 참조 방지
        Promise.race([
          stompClient.exitRoom(roomId),
          new Promise((_, reject) =>
            setTimeout(() => reject(new Error('Exit timeout')), 10000)
          ),
        ]).catch((error) => {
          console.error('[ROOM_CONNECTION] 언마운트 시 방 퇴장 실패:', error);
        }).finally(() => {
          setIsWebSocketConnected(false);
          setConnectionError(null);
        });
      }
    };
  }, [roomId, isAuthenticated]); // userId, nickname, avatarUrl 제거로 불필요한 재연결 방지

  return {
    isWebSocketConnected,
    connectionError,
    reconnect,
    disconnect,
  };
}