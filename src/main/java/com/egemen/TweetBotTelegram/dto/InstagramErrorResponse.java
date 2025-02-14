package com.egemen.TweetBotTelegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
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