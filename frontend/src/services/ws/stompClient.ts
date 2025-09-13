import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const resolveSockJsUrl = () => {
  const backendHost =
    (import.meta.env.VITE_BACKEND_HOST as string | undefined) ||
    window.location.hostname;
  const backendPort =
    (import.meta.env.VITE_BACKEND_PORT as string | undefined) ||
    (window.location.port === '5173' ? '8080' : window.location.port);

  const { protocol } = window.location;
  return `${protocol}//${backendHost}${backendPort ? `:${backendPort}` : ''}/ws`;
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

    this.client.onConnect = () => {
      this.isConnected = true;
      this.reconnectAttempt = 0;
      if (this.reconnectTimer) {
        window.clearTimeout(this.reconnectTimer);
        this.reconnectTimer = null;
      }
      console.log('STOMP connected');
      this.flushOfflineQueue();
      this.resubscribeAll();
    };

    this.client.onStompError = frame => {
      console.error('STOMP error:', frame.headers['message'], frame.body);
    };

    this.client.onWebSocketClose = event => {
      this.isConnected = false;
      console.warn('WebSocket closed:', event);
      // Only schedule reconnect if deactivate was not called explicitly
      if (this.client.active) {
        this.scheduleReconnect();
      }
    };

    this.client.onWebSocketError = event => {
      console.error('WebSocket error:', event);
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
    this.destinationToCallback.set(destination, callback);
    if (this.isConnected) {
      const sub = this.client.subscribe(destination, callback);
      this.destinationToSubscription.set(destination, sub);
      return sub;
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
      } catch (e) {
        console.error('Publish failed, queueing...', e);
        this.offlineQueue.push({ destination, body });
      }
    } else {
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
}

export const stompClient = StompClient.getInstance();
