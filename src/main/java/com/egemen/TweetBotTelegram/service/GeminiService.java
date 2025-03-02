package com.egemen.TweetBotTelegram.service;

public interface GeminiService {
    String generateImagePrompt(String title, String content) throws Exception;
    String generateSummary(String title, String content) throws Exception;
    String generateResponse(String prompt) throws Exception;
}
