package com.egemen.TweetBotTelegram.service;

public interface TelegramService {
    void sendMessage(String chatId, String message);
    void sendNewsUpdate(String chatId, String title, String content, String imageUrl);
    void sendError(String chatId, String errorMessage);
    void sendStatus(String chatId, String status);
} 