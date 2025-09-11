/// <reference types="vite/client" />

// 환경 변수 타입 선언 (Vite 전용)
// - PRD/TRD 명세에 맞춘 키 사용
interface ImportMetaEnv {
  readonly VITE_API_BASE: string; // 예: "/api"
  readonly VITE_WS_URL: string;   // 예: "ws://localhost/ws" 또는 "wss://domain/ws"
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
