import { useState, useReducer, useCallback } from 'react';
import { WebSocketMessageResponse, OnlineUser, MessageReaction } from '../../types/room';

// UI 상태 타입 정의
interface UIState {
  showLeaveModal: boolean;
  showUserList: boolean;
  showEmojiPicker: boolean;
  showContextMenu: boolean;
  showThreadView: boolean;
  contextMenuPosition: { x: number; y: number };
  activeActionsId: number | string | null;
  selectedMessage: WebSocketMessageResponse | null;
  threadParentMessage: WebSocketMessageResponse | null;
}

// UI 액션 타입 정의
type UIAction =
  | { type: 'TOGGLE_LEAVE_MODAL' }
  | { type: 'TOGGLE_USER_LIST' }
  | { type: 'TOGGLE_EMOJI_PICKER' }
  | { type: 'SHOW_CONTEXT_MENU'; payload: { x: number; y: number; message: WebSocketMessageResponse } }
  | { type: 'HIDE_CONTEXT_MENU' }
  | { type: 'SHOW_THREAD_VIEW'; payload: { parentMessage: WebSocketMessageResponse } }
  | { type: 'HIDE_THREAD_VIEW' }
  | { type: 'SET_ACTIVE_ACTIONS'; payload: number | string | null }
  | { type: 'RESET_UI' };

// 초기 상태
const initialState: UIState = {
  showLeaveModal: false,
  showUserList: false,
  showEmojiPicker: false,
  showContextMenu: false,
  showThreadView: false,
  contextMenuPosition: { x: 0, y: 0 },
  activeActionsId: null,
  selectedMessage: null,
  threadParentMessage: null,
};

// UI 상태 리듀서
function uiReducer(state: UIState, action: UIAction): UIState {
  switch (action.type) {
    case 'TOGGLE_LEAVE_MODAL':
      return { ...state, showLeaveModal: !state.showLeaveModal };

    case 'TOGGLE_USER_LIST':
      return { ...state, showUserList: !state.showUserList };

    case 'TOGGLE_EMOJI_PICKER':
      return { ...state, showEmojiPicker: !state.showEmojiPicker };

    case 'SHOW_CONTEXT_MENU':
      return {
        ...state,
        showContextMenu: true,
        contextMenuPosition: { x: action.payload.x, y: action.payload.y },
        selectedMessage: action.payload.message,
      };

    case 'HIDE_CONTEXT_MENU':
      return {
        ...state,
        showContextMenu: false,
        selectedMessage: null,
      };

    case 'SHOW_THREAD_VIEW':
      return {
        ...state,
        showThreadView: true,
        threadParentMessage: action.payload.parentMessage,
      };

    case 'HIDE_THREAD_VIEW':
      return {
        ...state,
        showThreadView: false,
        threadParentMessage: null,
      };

    case 'SET_ACTIVE_ACTIONS':
      return { ...state, activeActionsId: action.payload };

    case 'RESET_UI':
      return initialState;

    default:
      return state;
  }
}

// 메시지 리액션 관리는 이미 import된 타입 사용

export function useRoomUIState() {
  const [uiState, dispatch] = useReducer(uiReducer, initialState);

  // 메시지 리액션 상태 (별도 관리)
  const [messageReactions, setMessageReactions] = useState<Record<number, MessageReaction[]>>({});

  // 온라인 사용자 목록 (향후 실제 API 연동)
  const [onlineUsers, setOnlineUsers] = useState<OnlineUser[]>([]);

  // UI 액션 함수들
  const toggleLeaveModal = useCallback(() => {
    dispatch({ type: 'TOGGLE_LEAVE_MODAL' });
  }, []);

  const toggleUserList = useCallback(() => {
    dispatch({ type: 'TOGGLE_USER_LIST' });
  }, []);

  const toggleEmojiPicker = useCallback(() => {
    dispatch({ type: 'TOGGLE_EMOJI_PICKER' });
  }, []);

  const showContextMenu = useCallback((x: number, y: number, message: WebSocketMessageResponse) => {
    dispatch({ type: 'SHOW_CONTEXT_MENU', payload: { x, y, message } });
  }, []);

  const hideContextMenu = useCallback(() => {
    dispatch({ type: 'HIDE_CONTEXT_MENU' });
  }, []);

  const showThreadView = useCallback((parentMessage: WebSocketMessageResponse) => {
    dispatch({ type: 'SHOW_THREAD_VIEW', payload: { parentMessage } });
  }, []);

  const hideThreadView = useCallback(() => {
    dispatch({ type: 'HIDE_THREAD_VIEW' });
  }, []);

  const setActiveActions = useCallback((id: number | string | null) => {
    dispatch({ type: 'SET_ACTIVE_ACTIONS', payload: id });
  }, []);

  const resetUI = useCallback(() => {
    dispatch({ type: 'RESET_UI' });
    setMessageReactions({});
  }, []);

  // 메시지 리액션 관리 함수들
  const addMessageReaction = useCallback((messageId: number, emoji: string, user: string) => {
    setMessageReactions(prev => {
      const existing = prev[messageId] || [];
      const existingReaction = existing.find(r => r.emoji === emoji);

      if (existingReaction) {
        return {
          ...prev,
          [messageId]: existing.map(r =>
            r.emoji === emoji
              ? { ...r, count: r.count + 1, users: [...r.users, user] }
              : r
          )
        };
      } else {
        return {
          ...prev,
          [messageId]: [...existing, { emoji, count: 1, users: [user] }]
        };
      }
    });
  }, []);

  const removeMessageReaction = useCallback((messageId: number, emoji: string) => {
    setMessageReactions(prev => ({
      ...prev,
      [messageId]: (prev[messageId] || []).filter(r => r.emoji !== emoji)
    }));
  }, []);

  return {
    // UI 상태
    ...uiState,
    messageReactions,
    onlineUsers,

    // UI 액션 함수들
    toggleLeaveModal,
    toggleUserList,
    toggleEmojiPicker,
    showContextMenu,
    hideContextMenu,
    showThreadView,
    hideThreadView,
    setActiveActions,
    resetUI,

    // 메시지 리액션 함수들
    addMessageReaction,
    removeMessageReaction,

    // 온라인 사용자 관리
    setOnlineUsers,
  };
}