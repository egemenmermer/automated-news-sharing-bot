package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.service.NotificationService;
import com.egemen.TweetBotTelegram.service.TelegramService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final TelegramService telegramService;

    @Autowired
    public NotificationServiceImpl(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @Override
    public void sendNotification(String chatId, String message) {
        try {
            telegramService.sendMessage(chatId, message);
        } catch (Exception e) {
            log.error("Error sending notification: {}", e.getMessage());
        }
    }

    @Override
    public void sendError(String chatId, String errorMessage) {
        sendNotification(chatId, "❌ Error: " + errorMessage);
    }
}