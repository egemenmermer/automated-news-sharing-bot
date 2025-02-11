package com.egemen.TweetBotTelegram.dto.instagram;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InstagramMediaResponse {
    private String id;
    
    @JsonProperty("status_code")
    private String statusCode;
    
    private String status;
    
    @JsonProperty("error_message")
    private String errorMessage;
}