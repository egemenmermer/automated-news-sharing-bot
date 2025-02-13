package com.egemen.TweetBotTelegram.service;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface TelegramService {
    void sendMessage(String chatId, String message);
    void sendNewsUpdate(String chatId, String title, String content, String imageUrl);
    void sendError(String chatId, String errorMessage);
    void sendStatus(String chatId, String status);
    void onUpdateReceived(Update update);
} 