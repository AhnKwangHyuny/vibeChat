package com.vibechat.exception;

/**
 * 방 참가 시 닉네임 중복 충돌 예외
 * - 409로 매핑되며, 제안 닉네임(suggestedNickname)을 함께 제공한다.
 */
public class NicknameConflictException extends RuntimeException {

    private final String suggestedNickname;

    public NicknameConflictException(String message, String suggestedNickname) {
        super(message);
        this.suggestedNickname = suggestedNickname;
    }

    public String getSuggestedNickname() {
        return suggestedNickname;
    }
}


