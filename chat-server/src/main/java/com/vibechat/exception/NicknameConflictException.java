package com.vibechat.exception;

/**
 * 닉네임 중복 충돌 예외
 *
 * 방에서 같은 닉네임을 사용하는 사용자가 있을 때 발생
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


