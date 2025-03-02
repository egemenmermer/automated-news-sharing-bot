package com.egemen.TweetBotTelegram.exception;

import org.springframework.http.HttpStatus;

public class CustomException extends RuntimeException {
    private final HttpStatus status;
    private final String message;

    public CustomException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.message = message;
    }

    public CustomException(Exception e, HttpStatus status) {
        super(e.getMessage());
        this.status = status;
        this.message = e.getMessage();
    }

    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}