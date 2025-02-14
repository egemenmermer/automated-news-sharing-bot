package com.egemen.TweetBotTelegram.service;

public interface NotificationService {
    void sendNotification(String chatId, String message);
    void sendError(String chatId, String errorMessage);
}