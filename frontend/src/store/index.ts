import { configureStore } from '@reduxjs/toolkit';
import userReducer from './userSlice';
import messagesReducer from '../features/messages/messageSlice';
import presenceReducer from '../features/rooms/presenceSlice';
import typingReducer from '../features/messages/typingSlice';

export const store = configureStore({
    reducer: {
        user: userReducer,
        messages: messagesReducer,
        presence: presenceReducer,
        typing: typingReducer,
        // Add other reducers here
    },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
