package com.egemen.TweetBotTelegram.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {
    private DetailedError detailedError;
    private String statusCode;
    private HttpServletRequest request;
    private HttpStatus status;

    public CustomException(Exception e, HttpStatus status) {
        super(e.getMessage());
        this.detailedError = new DetailedError();
        this.detailedError.setMessage(e.getMessage());
        this.statusCode = status.toString();
    }
}