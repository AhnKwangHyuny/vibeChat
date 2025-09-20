package com.vibechat.exception.tag;

public class InvalidTagNameException extends RuntimeException {
    public InvalidTagNameException(String message) {
        super(message);
    }
    
    public InvalidTagNameException(String message, Throwable cause) {
        super(message, cause);
    }
}
