import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface UserState {
    id: string | null;
    nickname: string | null;
}

const initialState: UserState = {
    id: null,
    nickname: null,
};

const userSlice = createSlice({
    name: 'user',
    initialState,
    reducers: {
        setUser: (state, action: PayloadAction<UserState>) => {
            state.id = action.payload.id;
            state.nickname = action.payload.nickname;
        },
        clearUser: (state) => {
            state.id = null;
            state.nickname = null;
        },
    },
});

export const { setUser, clearUser } = userSlice.actions;
export default userSlice.reducer;
