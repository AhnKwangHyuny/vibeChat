import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface TypingState {
  [roomId: string]: string[]; // roomId: array of nicknames typing
}

const initialState: TypingState = {};

const typingSlice = createSlice({
  name: 'typing',
  initialState,
  reducers: {
    updateTypingUser: (state, action: PayloadAction<{ roomId: number; nickname: string; isTyping: boolean }>) => {
      const { roomId, nickname, isTyping } = action.payload;
      const roomTyping = state[roomId] || [];
      const userIndex = roomTyping.indexOf(nickname);

      if (isTyping && userIndex === -1) {
        roomTyping.push(nickname);
      } else if (!isTyping && userIndex !== -1) {
        roomTyping.splice(userIndex, 1);
      }

      state[roomId] = roomTyping;
    },
  },
});

export const { updateTypingUser } = typingSlice.actions;
export default typingSlice.reducer;
