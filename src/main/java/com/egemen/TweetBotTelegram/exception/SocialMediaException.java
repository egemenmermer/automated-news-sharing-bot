package com.egemen.TweetBotTelegram.exception;

import org.springframework.http.HttpStatus;

public class SocialMediaException extends CustomException {
    public SocialMediaException(String message, HttpStatus status) {
        super(message, status);
    }
}