package com.egemen.TweetBotTelegram.dto.instagram;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InstagramErrorResponse {
    private Error error;

    @Data
    public static class Error {
        private String message;
        private String type;
        private String code;
        
        @JsonProperty("error_subcode")
        private String errorSubcode;
    }
}