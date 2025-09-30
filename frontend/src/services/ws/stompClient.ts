import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const resolveSockJsUrl = () => {
  // Chat-Server WebSocket 연결 (SockJS는 http/https 프로토콜 사용)
  const chatServerHost =
    (import.meta.env.VITE_CHAT_SERVER_HOST as string | undefined) ||
    window.location.hostname;
  const chatServerPort =
    (import.meta.env.VITE_CHAT_SERVER_PORT as string | undefined) ||
    '8081'; // Chat-Server 기본 포트

  const { protocol } = window.location;
  // SockJS는 HTTP/HTTPS 프로토콜을 사용해야 함
  const httpProtocol = protocol === 'https:' ? 'https:' : 'http:';
  const url = `${httpProtocol}//${chatServerHost}:${chatServerPort}/ws`;

  console.log('SockJS connecting to Chat-Server:', url);
  return url;
};

class StompClient {
  private client: Client;
  private static instance: StompClient;

  private isConnected: boolean = false;
  private reconnectAttempt: number = 0;
  private readonly maxReconnectDelayMs: number = 10000;
  private reconnectTimer: number | null = null;

  private destinationToCallback: Map<string, (message: IMessage) => void> =
    new Map();
  private destinationToSubscription: Map<string, StompSubscription> = new Map();
  private offlineQueue: Array<{ destination: string; body: string }> = [];

  private constructor() {
    this.client = new Client({
      debug: str => {
        if (import.meta.env.MODE === 'development') {
          console.log('[STOMP]', str);
        }
      },
      reconnectDelay: 0,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    this.client.webSocketFactory = () => new SockJS(resolveSockJsUrl());

    this.client.onConnect = (frame) => {
      this.isConnected = true;
      this.reconnectAttempt = 0;
      if (this.reconnectTimer) {
        window.clearTimeout(this.reconnectTimer);
        this.reconnectTimer = null;
      }
      console.log('✅ STOMP WebSocket 연결 성공!', {
        server: frame.headers.server || 'Chat-Server',
        version: frame.headers.version || 'unknown',
        headers: frame.headers
      });
      this.flushOfflineQueue();
      this.resubscribeAll();
    };

    this.client.onStompError = frame => {
      console.error('❌ STOMP 오류 발생:', {
        message: frame.headers['message'],
        body: frame.body,
        headers: frame.headers
      });
    };

    this.client.onWebSocketClose = event => {
      this.isConnected = false;
      console.warn('⚠️ WebSocket 연결 종료:', {
        code: event.code,
        reason: event.reason,
        wasClean: event.wasClean,
        type: event.type
      });
      // Only schedule reconnect if deactivate was not called explicitly
      if (this.client.active) {
        console.log('🔄 자동 재연결 시도 예약 중...');
        this.scheduleReconnect();
      }
    };

    this.client.onWebSocketError = event => {
      console.error('❌ WebSocket 연결 오류:', {
        error: event,
        type: event.type,
        target: event.target
      });
    };
  }

  private activate() {
    try {
      this.client.activate();
    } catch (e) {
      console.error('Failed to activate STOMP client:', e);
      this.scheduleReconnect();
    }
  }

  public connect() {
    if (!this.isConnected && !this.client.active) {
      this.activate();
    }
  }

  private scheduleReconnect() {
    if (this.reconnectTimer) return;
    const delay = Math.min(
      1000 * Math.pow(2, this.reconnectAttempt),
      this.maxReconnectDelayMs
    );
    this.reconnectAttempt += 1;
    console.log(
      `Scheduling STOMP reconnect in ${delay}ms (attempt ${this.reconnectAttempt})`
    );
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null;
      this.activate();
    }, delay);
  }

  private flushOfflineQueue() {
    if (!this.isConnected || this.offlineQueue.length === 0) return;
    const pending = [...this.offlineQueue];
    this.offlineQueue = [];
    pending.forEach(msg => {
      try {
        this.client.publish({ destination: msg.destination, body: msg.body });
      } catch (e) {
        console.error('Failed to publish queued message, re-queueing', e);
        this.offlineQueue.push(msg);
      }
    });
  }

  private resubscribeAll() {
    this.destinationToCallback.forEach((callback, destination) => {
      const existing = this.destinationToSubscription.get(destination);
      if (existing) {
        try {
          existing.unsubscribe();
        } catch {}
      }
      const sub = this.client.subscribe(destination, callback);
      this.destinationToSubscription.set(destination, sub);
    });
  }

  public static getInstance(): StompClient {
    if (!StompClient.instance) {
      StompClient.instance = new StompClient();
    }
    return StompClient.instance;
  }

  public subscribe(
    destination: string,
    callback: (message: IMessage) => void
  ): StompSubscription | null {
    console.log(`📡 WebSocket 구독 등록: ${destination}`);
    this.destinationToCallback.set(destination, callback);
    if (this.isConnected) {
      const sub = this.client.subscribe(destination, callback);
      this.destinationToSubscription.set(destination, sub);
      console.log(`✅ WebSocket 구독 성공: ${destination}`);
      return sub;
    } else {
      console.log(`⏳ WebSocket 연결 대기 중 - 구독 예약: ${destination}`);
    }
    return null;
  }

  public unsubscribe(destination: string) {
    const sub = this.destinationToSubscription.get(destination);
    if (sub) {
      try {
        sub.unsubscribe();
      } catch {}
      this.destinationToSubscription.delete(destination);
    }
    this.destinationToCallback.delete(destination);
  }

  public publish(destination: string, body: string) {
    if (this.isConnected) {
      try {
        this.client.publish({ destination, body });
        console.log(`📤 메시지 전송 성공: ${destination}`);
      } catch (e) {
        console.error('📤❌ 메시지 전송 실패, 대기열에 추가:', e);
        this.offlineQueue.push({ destination, body });
      }
    } else {
      console.log(`📤⏳ WebSocket 연결 대기 중 - 메시지 대기열에 추가: ${destination}`);
      this.offlineQueue.push({ destination, body });
    }
  }

  public disconnect() {
    try {
      if (this.reconnectTimer) {
        window.clearTimeout(this.reconnectTimer);
        this.reconnectTimer = null;
      }
      this.client.deactivate();
    } finally {
      this.isConnected = false;
    }
  }

  public get connected(): boolean {
    return this.isConnected;
  }

  public sendRoomMessage(roomId: number, payload: unknown) {
    const destination = `/app/rooms/${roomId}/send`;
    const body = JSON.stringify(payload);

    console.log(`🎯 [DEBUG] sendRoomMessage 호출:`, {
      roomId,
      destination,
      payload,
      isConnected: this.isConnected,
      clientActive: this.client?.active
    });

    this.publish(destination, body);
  }

  public sendTyping(roomId: number, typing: boolean) {
    this.publish(`/app/rooms/${roomId}/typing`, JSON.stringify({ typing }));
  }

  public joinRoom(roomId: number): Promise<void> {
    console.log(`🏠 방 입장 요청: ${roomId}`);
    return new Promise((resolve, reject) => {
      let isResolved = false;

      // 성공 응답 구독
      const responseSubscription = this.subscribe('/queue/room-join-response', (message) => {
        if (isResolved) return;

        try {
          const response = JSON.parse(message.body);
          console.log(`📨 방 입장 응답 수신:`, response);

          if (response.type === 'ROOM_JOIN_SUCCESS' && response.roomId === roomId) {
            console.log(`✅ 방 입장 성공: ${roomId}`, response);
            isResolved = true;
            responseSubscription?.unsubscribe();
            errorSubscription?.unsubscribe();
            resolve();
          }
        } catch (e) {
          console.error('방 입장 응답 파싱 실패:', e);
        }
      });

      // 에러 응답 구독
      const errorSubscription = this.subscribe('/queue/errors', (message) => {
        if (isResolved) return;

        try {
          const error = JSON.parse(message.body);
          if (error.type === 'ROOM_JOIN_FAILED') {
            console.error(`❌ 방 입장 에러: ${roomId}`, error);
            isResolved = true;
            responseSubscription?.unsubscribe();
            errorSubscription?.unsubscribe();
            reject(new Error(error.detail || '방 입장에 실패했습니다'));
          }
        } catch (e) {
          console.error('에러 응답 파싱 실패:', e);
        }
      });

      // 타임아웃 설정 (15초로 연장)
      const timeoutId = setTimeout(() => {
        if (!isResolved) {
          console.warn(`⏰ 방 입장 타임아웃: ${roomId}`);
          isResolved = true;
          responseSubscription?.unsubscribe();
          errorSubscription?.unsubscribe();
          reject(new Error('방 입장 요청 시간 초과'));
        }
      }, 15000);

      // 실제 방 입장 요청 전송
      try {
        this.publish(`/app/rooms/${roomId}/join`, JSON.stringify({
          roomId: roomId,
          timestamp: new Date().toISOString()
        }));
        console.log(`📤 방 입장 요청 전송 완료: ${roomId}`);
      } catch (error) {
        console.error(`❌ 방 입장 요청 전송 실패: ${roomId}`, error);
        clearTimeout(timeoutId);
        responseSubscription?.unsubscribe();
        errorSubscription?.unsubscribe();
        reject(error);
      }
    });
  }

  public leaveRoom(roomId: number): Promise<void> {
    console.log(`🚪 방 퇴장 요청: ${roomId}`);
    return new Promise((resolve, reject) => {
      let isResolved = false;

      // 성공 응답 구독
      const responseSubscription = this.subscribe('/queue/room-leave-response', (message) => {
        if (isResolved) return;

        try {
          const response = JSON.parse(message.body);
          console.log(`📨 방 퇴장 응답 수신:`, response);

          if (response.type === 'ROOM_LEAVE_SUCCESS' && response.roomId === roomId) {
            console.log(`✅ 방 퇴장 성공: ${roomId}`, response);
            isResolved = true;
            responseSubscription?.unsubscribe();
            errorSubscription?.unsubscribe();
            resolve();
          }
        } catch (e) {
          console.error('방 퇴장 응답 파싱 실패:', e);
        }
      });

      // 에러 응답 구독
      const errorSubscription = this.subscribe('/queue/errors', (message) => {
        if (isResolved) return;

        try {
          const error = JSON.parse(message.body);
          if (error.type === 'ROOM_LEAVE_FAILED') {
            console.error(`❌ 방 퇴장 에러: ${roomId}`, error);
            isResolved = true;
            responseSubscription?.unsubscribe();
            errorSubscription?.unsubscribe();
            reject(new Error(error.detail || '방 퇴장에 실패했습니다'));
          }
        } catch (e) {
          console.error('에러 응답 파싱 실패:', e);
        }
      });

      // 타임아웃 설정 (8초로 조정)
      const timeoutId = setTimeout(() => {
        if (!isResolved) {
          console.warn(`⏰ 방 퇴장 타임아웃: ${roomId}`);
          isResolved = true;
          responseSubscription?.unsubscribe();
          errorSubscription?.unsubscribe();
          reject(new Error('방 퇴장 요청 시간 초과'));
        }
      }, 8000);

      // 실제 방 퇴장 요청 전송
      try {
        this.publish(`/app/rooms/${roomId}/leave`, JSON.stringify({
          roomId: roomId,
          timestamp: new Date().toISOString()
        }));
        console.log(`📤 방 퇴장 요청 전송 완료: ${roomId}`);
      } catch (error) {
        console.error(`❌ 방 퇴장 요청 전송 실패: ${roomId}`, error);
        clearTimeout(timeoutId);
        responseSubscription?.unsubscribe();
        errorSubscription?.unsubscribe();
        reject(error);
      }
    });
  }

  /**
   * 통합 방 입장 워크플로우
   * WebSocket 연결 + 방 입장 + 자동 구독을 하나의 플로우로 처리
   */
  public async enterRoom(roomId: number, userId: string, nickname: string, avatarUrl?: string): Promise<void> {
    try {
      console.log(`🚀 통합 방 입장 워크플로우 시작: roomId=${roomId}, userId=${userId}`);

      // Step 1: WebSocket 연결 확인/설정
      if (!this.connected) {
        await this.connectWithUser(userId, nickname, avatarUrl);
      }

      // Step 2: 방 입장 요청 (서버에서 동적 스트림 생성 및 참가자 등록)
      await this.joinRoom(roomId);

      // Step 3: 방 스트림 자동 구독
      await this.subscribeToRoomStreams(roomId);

      console.log(`✅ 통합 방 입장 워크플로우 완료: roomId=${roomId}`);
    } catch (error) {
      console.error(`❌ 통합 방 입장 워크플로우 실패: roomId=${roomId}`, error);
      throw error;
    }
  }

  /**
   * 방 스트림들을 자동으로 구독
   */
  private async subscribeToRoomStreams(roomId: number): Promise<void> {
    try {
      console.log(`📡 방 스트림 구독 시작: roomId=${roomId}`);

      // 방 메시지 스트림 구독 (이미 useMessages에서 처리중이므로 중복 방지)
      // this.subscribe(`/topic/rooms/${roomId}/messages`, this.handleRoomMessage);

      // Presence 스트림 구독
      this.subscribe(`/topic/rooms/${roomId}/presence`, (message) => {
        console.log(`👥 Presence 업데이트: roomId=${roomId}`, JSON.parse(message.body));
      });

      // 타이핑 인디케이터 구독
      this.subscribe(`/topic/rooms/${roomId}/typing`, (message) => {
        console.log(`⌨️ 타이핑 상태: roomId=${roomId}`, JSON.parse(message.body));
      });

      console.log(`✅ 방 스트림 구독 완료: roomId=${roomId}`);
    } catch (error) {
      console.error(`❌ 방 스트림 구독 실패: roomId=${roomId}`, error);
      throw error;
    }
  }

  /**
   * 통합 방 퇴장 워크플로우
   */
  public async exitRoom(roomId: number): Promise<void> {
    try {
      console.log(`🚪 통합 방 퇴장 워크플로우 시작: roomId=${roomId}`);

      // Step 1: 방 퇴장 요청 (서버에서 참가자 제거)
      await this.leaveRoom(roomId);

      // Step 2: 방 관련 구독 해제
      this.unsubscribe(`/topic/rooms/${roomId}/messages`);
      this.unsubscribe(`/topic/rooms/${roomId}/presence`);
      this.unsubscribe(`/topic/rooms/${roomId}/typing`);

      console.log(`✅ 통합 방 퇴장 워크플로우 완료: roomId=${roomId}`);
    } catch (error) {
      console.error(`❌ 통합 방 퇴장 워크플로우 실패: roomId=${roomId}`, error);
      throw error;
    }
  }

  public connectWithUser(userId: string, nickname: string, avatarUrl?: string): Promise<void> {
    return new Promise((resolve, reject) => {
      // 연결 헤더에 사용자 정보 포함
      const connectHeaders = {
        userId: userId,
        nickname: nickname,
        avatarUrl: avatarUrl || ''
      };

      console.log('🚀 Chat-Server WebSocket 연결 시작:', {
        url: resolveSockJsUrl(),
        user: connectHeaders,
        timestamp: new Date().toISOString()
      });

      // 임시 이벤트 리스너 설정
      const originalOnConnect = this.client.onConnect;
      const originalOnStompError = this.client.onStompError;

      this.client.onConnect = (frame) => {
        console.log('✅ Chat-Server 연결 완료!', {
          frame: frame,
          user: connectHeaders,
          connectionTime: new Date().toISOString()
        });
        // 원래 핸들러 복원
        this.client.onConnect = originalOnConnect;
        this.client.onStompError = originalOnStompError;
        // 연결 성공 시 원래 핸들러 호출
        if (originalOnConnect) originalOnConnect(frame);
        resolve();
      };

      this.client.onStompError = (frame) => {
        console.error('❌ Chat-Server 연결 실패:', {
          frame: frame,
          user: connectHeaders,
          error: frame.headers.message
        });
        // 원래 핸들러 복원
        this.client.onConnect = originalOnConnect;
        this.client.onStompError = originalOnStompError;
        reject(new Error(`WebSocket connection failed: ${frame.headers.message}`));
      };

      // 헤더와 함께 연결 시작
      this.client.connectHeaders = connectHeaders;
      this.client.activate();
    });
  }

  public activate() {
    this.client.activate();
  }
}

export const stompClient = StompClient.getInstance();
