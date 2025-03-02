package com.egemen.TweetBotTelegram.dto;

import com.egemen.TweetBotTelegram.enums.NewsStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewsListDTO {
    private String title;
    private String description;
    private Timestamp publishedAt;
    private NewsStatus status;
}
