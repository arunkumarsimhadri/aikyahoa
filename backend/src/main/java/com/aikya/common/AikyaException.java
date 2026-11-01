package com.aikya.common;

import org.springframework.http.HttpStatus;

public class AikyaException extends RuntimeException {

    private final HttpStatus status;

    public AikyaException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

