package com.buyukozkan.boilerplate.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("Token is invalid or expired");
    }
}
