package com.egemen.TweetBotTelegram.dto;

import com.egemen.TweetBotTelegram.enums.SummarizedStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SummarizedNewsDTO {
    private String content;
    private int summarizedCount;
    private SummarizedStatus status;
}
