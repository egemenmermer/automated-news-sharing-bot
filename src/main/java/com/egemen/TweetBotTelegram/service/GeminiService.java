package com.egemen.TweetBotTelegram.service;

public interface GeminiService {
    String summarizeNews(String newsTitle, String newsContent);
    String generateTweet(String newsSummary);
    String generateSummary(String title, String content);
    String generateImagePrompt(String title, String content);
    String generateCaption(String title, String content);
    String generateResponse(String prompt);
}
