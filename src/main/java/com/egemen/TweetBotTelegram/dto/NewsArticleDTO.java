package com.egemen.TweetBotTelegram.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewsArticleDTO {
    private String title;
    private String description;
    private String url;
    private String image;
    private String publishedAt;
    private String source;

} 