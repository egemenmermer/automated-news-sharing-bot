package com.egemen.TweetBotTelegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InstagramMediaResponseDTO {
    private String id;
    
    @JsonProperty("status_code")
    private String statusCode;
    
    private String status;
    
    @JsonProperty("error_message")
    private String errorMessage;



}