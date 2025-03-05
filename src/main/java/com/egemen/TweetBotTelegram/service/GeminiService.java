package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.News;

public interface GeminiService {
    String generateImagePrompt(String title, String content) throws Exception;
    String generateSummary(String title, String content) throws Exception;
    String generateResponse(String prompt) throws Exception;
    String generateImageForNews(News news) throws Exception;
    String generateInstagramCaption(News news) throws Exception;
    
    String generateDetailedSummary(String title, String content, int maxLength);
    String generateShortSummary(String title, int maxLength);
}
