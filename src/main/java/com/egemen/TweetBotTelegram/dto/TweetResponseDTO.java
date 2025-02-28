package com.egemen.TweetBotTelegram.dto;

import com.egemen.TweetBotTelegram.enums.TweetStatus;

import java.sql.Timestamp;

@lombok.Data
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class TweetResponseDTO {
    private String content;
    private TweetStatus status;
    private Timestamp scheduledAt;

}