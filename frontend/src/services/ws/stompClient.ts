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
    this.publish(`/app/rooms/${roomId}/send`, JSON.stringify(payload));
  }

  public sendTyping(roomId: number, typing: boolean) {
    this.publish(`/app/rooms/${roomId}/typing`, JSON.stringify({ typing }));
  }

  public joinRoom(roomId: number) {
    console.log(`🏠 방 입장 요청: ${roomId}`);
    this.publish(`/app/rooms/${roomId}/join`, JSON.stringify({}));
  }

  public leaveRoom(roomId: number) {
    console.log(`🚪 방 퇴장 요청: ${roomId}`);
    this.publish(`/app/rooms/${roomId}/leave`, JSON.stringify({}));
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
