package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.SummarizedNews;
import com.egemen.TweetBotTelegram.entity.Tweet;

import java.util.List;

public interface GeminiService {
    String summarizeNews(String newsTitle, String newsContent);
    String generateTweet(String newsSummary);
    List<SummarizedNews> summarize() throws InterruptedException;
    List<Tweet> generate() throws InterruptedException;
    void start() throws InterruptedException;
    void refreshAccessToken();
    void checkAndRefreshToken();
}
