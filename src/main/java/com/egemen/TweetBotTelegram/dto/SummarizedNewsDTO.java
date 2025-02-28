package com.egemen.TweetBotTelegram.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SummarizedNewsDTO {
    private String content;
    private int summarizedCount;
    private SummarizeStatus status;
}
