package com.vibechat.exception.tag;

public class TagNotFoundException extends RuntimeException {
    public TagNotFoundException(String message) {
        super(message);
    }
    
    public TagNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
