package com.zn.exception;

public class AdminAuthenticationException extends RuntimeException {
    public AdminAuthenticationException(String message) {
        super(message);
    }

    public AdminAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
