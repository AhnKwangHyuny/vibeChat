import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

/**
 * STOMP 클라이언트 싱글턴
 * - SockJS + STOMP 사용 (Spring STOMP와 호환)
 * - 지수 백오프(1s→2s→4s→... 최대 10s) 재연결
 * - 오프라인 전송 큐: 연결 끊김 동안 publish 요청 저장 후 재연결 시 플러시
 * - 구독 자동 복원: 재연결 시 기존 구독 재설정
 */
class StompClient {
  private client: Client;
  private static instance: StompClient;

  // 연결/재연결 상태 관리
  private isConnected: boolean = false;
  private reconnectAttempt: number = 0;
  private readonly maxReconnectDelayMs: number = 10000;
  private reconnectTimer: number | null = null;

  // 구독/전송 관리
  private destinationToCallback: Map<string, (message: IMessage) => void> = new Map();
  private destinationToSubscription: Map<string, StompSubscription> = new Map();
  private offlineQueue: Array<{ destination: string; body: string } > = [];

  private constructor() {
    // Vite 환경변수: ws(s) URL (예: ws://localhost/ws)
    // SockJS는 http(s) 스킴을 사용하므로 변환
    const wsUrl = import.meta.env.VITE_WS_URL || `ws://${window.location.host}/ws`;
    const sockJsUrl = wsUrl.replace('ws://', 'http://').replace('wss://', 'https://');

    // brokerURL은 WebSocket 직접 연결 시 사용.
    // SockJS를 사용할 것이므로 webSocketFactory만 지정하고 brokerURL은 설정하지 않음.
    this.client = new Client({
      // brokerURL: wsUrl, // SockJS 사용할 때는 미설정
      debug: (str) => {
        if (import.meta.env.MODE === 'development') {
          console.log('[STOMP]', str);
        }
      },
      // 라이브러리 기본 재연결은 고정 지연이므로 비활성화하고 커스텀 지수 백오프 사용
      reconnectDelay: 0,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    this.client.webSocketFactory = () => new SockJS(sockJsUrl);

    // 연결 성공 시 콜백
    this.client.onConnect = () => {
      this.isConnected = true;
      this.reconnectAttempt = 0;
      if (this.reconnectTimer) {
        window.clearTimeout(this.reconnectTimer);
        this.reconnectTimer = null;
      }
      console.log('STOMP connected');

      // 끊김 동안 큐에 쌓인 전송을 플러시
      this.flushOfflineQueue();

      // 재연결 시 기존 구독 재설정
      this.resubscribeAll();
    };

    this.client.onStompError = (frame) => {
      console.error('STOMP error:', frame.headers['message'], frame.body);
    };

    this.client.onWebSocketClose = (event) => {
      this.isConnected = false;
      console.warn('WebSocket closed:', event);
      this.scheduleReconnect();
    };

    this.client.onWebSocketError = (event) => {
      console.error('WebSocket error:', event);
    };

    this.activate();
  }

  // 내부 활성화 래퍼
  private activate() {
    try {
      this.client.activate();
    } catch (e) {
      console.error('Failed to activate STOMP client:', e);
      this.scheduleReconnect();
    }
  }

  // 지수 백오프 재연결 스케줄
  private scheduleReconnect() {
    if (this.reconnectTimer) return; // 이미 예약됨
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempt), this.maxReconnectDelayMs);
    this.reconnectAttempt += 1;
    console.log(`Scheduling STOMP reconnect in ${delay}ms (attempt ${this.reconnectAttempt})`);
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null;
      this.activate();
    }, delay);
  }

  // 오프라인 큐 플러시
  private flushOfflineQueue() {
    if (!this.isConnected || this.offlineQueue.length === 0) return;
    const pending = [...this.offlineQueue];
    this.offlineQueue = [];
    pending.forEach((msg) => {
      try {
        this.client.publish({ destination: msg.destination, body: msg.body });
      } catch (e) {
        console.error('Failed to publish queued message, re-queueing', e);
        this.offlineQueue.push(msg);
      }
    });
  }

  // 모든 목적지 재구독
  private resubscribeAll() {
    this.destinationToCallback.forEach((callback, destination) => {
      // 기존 구독 핸들 해제 후 재구독
      const existing = this.destinationToSubscription.get(destination);
      if (existing) {
        try { existing.unsubscribe(); } catch {}
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

  /**
   * 구독 등록
   * - 재연결 시 자동으로 복원됨
   */
  public subscribe(destination: string, callback: (message: IMessage) => void): StompSubscription | null {
    this.destinationToCallback.set(destination, callback);
    if (this.isConnected) {
      const sub = this.client.subscribe(destination, callback);
      this.destinationToSubscription.set(destination, sub);
      return sub;
    }
    // 아직 연결 전인 경우, 연결 후 resubscribeAll에서 처리됨
    return null;
  }

  /**
   * 구독 해제 및 관리 맵 정리
   */
  public unsubscribe(destination: string) {
    const sub = this.destinationToSubscription.get(destination);
    if (sub) {
      try { sub.unsubscribe(); } catch {}
      this.destinationToSubscription.delete(destination);
    }
    this.destinationToCallback.delete(destination);
  }

  /**
   * 메시지 발행
   * - 오프라인 시 큐에 저장 후 재연결 시 플러시
   */
  public publish(destination: string, body: string) {
    if (this.isConnected) {
      try {
        this.client.publish({ destination, body });
      } catch (e) {
        console.error('Publish failed, queueing...', e);
        this.offlineQueue.push({ destination, body });
      }
    } else {
      this.offlineQueue.push({ destination, body });
    }
  }

  /** 연결 해제 */
  public disconnect() {
    try {
      this.client.deactivate();
    } finally {
      this.isConnected = false;
      if (this.reconnectTimer) {
        window.clearTimeout(this.reconnectTimer);
        this.reconnectTimer = null;
      }
    }
  }

  /** 연결 여부 조회 */
  public get connected(): boolean {
    return this.isConnected;
  }

  /** 편의 메서드: 채팅방 메시지 전송 */
  public sendRoomMessage(roomId: number, payload: unknown) {
    this.publish(`/app/rooms/${roomId}/send`, JSON.stringify(payload));
  }

  /** 편의 메서드: 타이핑 상태 전송 */
  public sendTyping(roomId: number, typing: boolean) {
    this.publish(`/app/rooms/${roomId}/typing`, JSON.stringify({ typing }));
  }
}

export const stompClient = StompClient.getInstance();
