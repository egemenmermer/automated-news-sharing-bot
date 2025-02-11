package com.egemen.TweetBotTelegram.service;

public interface GeminiService {
    String generateImagePrompt(String title, String content);
    String generateCaption(String title, String content);
    String generateResponse(String prompt);
}
