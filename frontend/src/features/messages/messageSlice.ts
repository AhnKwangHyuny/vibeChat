import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface WebSocketMessageResponse {
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

interface MessagesState {
  [roomId: string]: {
    items: WebSocketMessageResponse[];
    hasMore: boolean;
    loading: boolean;
    pendingByClientTempId: { [clientTempId: string]: WebSocketMessageResponse };
  };
}

const initialState: MessagesState = {};

const messagesSlice = createSlice({
  name: 'messages',
  initialState,
  reducers: {
    addMessage: (state, action: PayloadAction<WebSocketMessageResponse>) => {
      const { roomId, clientTempId } = action.payload;
      if (!state[roomId]) {
        state[roomId] = { items: [], hasMore: true, loading: false, pendingByClientTempId: {} };
      }
      // If it's a pending message, add to pending map
      if (clientTempId && !action.payload.id) {
        state[roomId].pendingByClientTempId[clientTempId] = action.payload;
      } else if (clientTempId && action.payload.id) {
        // If it's a confirmed message, remove from pending and add to items
        delete state[roomId].pendingByClientTempId[clientTempId];
        state[roomId].items.push(action.payload);
      } else {
        state[roomId].items.push(action.payload);
      }
    },
    setMessages: (state, action: PayloadAction<{ roomId: number; messages: WebSocketMessageResponse[]; hasMore: boolean }>) => {
      const { roomId, messages, hasMore } = action.payload;
      if (!state[roomId]) {
        state[roomId] = { items: [], hasMore: true, loading: false, pendingByClientTempId: {} };
      }
      state[roomId].items = messages;
      state[roomId].hasMore = hasMore;
      state[roomId].loading = false;
    },
    setLoading: (state, action: PayloadAction<{ roomId: number; loading: boolean }>) => {
      const { roomId, loading } = action.payload;
      if (!state[roomId]) {
        state[roomId] = { items: [], hasMore: true, loading: false, pendingByClientTempId: {} };
      }
      state[roomId].loading = loading;
    },
    // Action to update a pending message to a confirmed message
    messageConfirmed: (state, action: PayloadAction<WebSocketMessageResponse>) => {
      const { roomId, clientTempId, id } = action.payload;
      if (state[roomId] && clientTempId && id) {
        // Remove from pending
        delete state[roomId].pendingByClientTempId[clientTempId];
        // Add to items
        state[roomId].items.push(action.payload);
      }
    },
  },
});

export const { addMessage, setMessages, setLoading, messageConfirmed } = messagesSlice.actions;
export default messagesSlice.reducer;
