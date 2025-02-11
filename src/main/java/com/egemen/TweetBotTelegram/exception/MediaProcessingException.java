package com.egemen.TweetBotTelegram.exception;

public class InstagramApiException extends RuntimeException {
    public InstagramApiException(String message) {
        super(message);
    }

    public InstagramApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Add specific exceptions for different scenarios
public class MediaProcessingException extends InstagramApiException {
    public MediaProcessingException(String message) {
        super(message);
    }
}