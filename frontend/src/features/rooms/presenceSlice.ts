import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface PresenceState {
  [roomId: string]: number; // roomId: onlineCount
}

const initialState: PresenceState = {};

const presenceSlice = createSlice({
  name: 'presence',
  initialState,
  reducers: {
    setOnlineCount: (state, action: PayloadAction<{ roomId: number; count: number }>) => {
      state[action.payload.roomId] = action.payload.count;
    },
  },
});

export const { setOnlineCount } = presenceSlice.actions;
export default presenceSlice.reducer;
