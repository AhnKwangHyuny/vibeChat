import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface UserState {
    id: string | null;
    nickname: string | null;
    avatarUrl: string | null;
    provider: 'GUEST' | 'GOOGLE' | null;
}

const initialState: UserState = {
    id: null,
    nickname: null,
    avatarUrl: null,
    provider: null,
};

const userSlice = createSlice({
    name: 'user',
    initialState,
    reducers: {
        setUser: (state, action: PayloadAction<Partial<UserState>>) => {
            Object.assign(state, action.payload);
        },
        clearUser: (state) => {
            state.id = null;
            state.nickname = null;
            state.avatarUrl = null;
            state.provider = null;
        },
    },
});

export const { setUser, clearUser } = userSlice.actions;
export default userSlice.reducer;
