package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.service.TelegramService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Service
public class TelegramServiceImpl extends TelegramLongPollingBot implements TelegramService {

    private static final String DEFAULT_CHAT_ID = "YOUR_CHAT_ID"; // We'll get this when you first interact with the bot

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();
            
            log.info("Received message: '{}' from chat ID: {}", messageText, chatId);

            if (messageText.equals("/start")) {
                sendMessage(chatId, "Welcome to Neural News Bot! 🤖\n\nI'll keep you updated with the latest news.");
            } else if (messageText.equals("/help")) {
                sendMessage(chatId, """
                    Available commands:
                    /start - Start the bot
                    /help - Show this help message
                    /latest - Get latest news
                    /status - Get bot status
                    """);
            } else if (messageText.equals("/latest")) {
                sendMessage(chatId, "🔍 Fetching latest news...");
                // TODO: Implement latest news fetch
            } else if (messageText.equals("/status")) {
                sendMessage(chatId, "📊 Bot is running normally");
            }
        }
    }

    @Override
    public void sendMessage(String chatId, String message) {
        try {
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId)
                    .text(message)
                    .parseMode("HTML")
                    .build();
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error sending message to Telegram: {}", e.getMessage());
        }
    }

    @Override
    public void sendNewsUpdate(String chatId, String title, String content, String imageUrl) {
        try {
            // First send the image if available
            if (imageUrl != null && !imageUrl.isEmpty()) {
                SendPhoto sendPhoto = SendPhoto.builder()
                        .chatId(chatId)
                        .photo(new InputFile(imageUrl))
                        .caption(String.format("<b>%s</b>\n\n%s", title, content))
                        .parseMode("HTML")
                        .build();
                execute(sendPhoto);
            } else {
                // If no image, send text only
                sendMessage(chatId, String.format("<b>%s</b>\n\n%s", title, content));
            }
        } catch (TelegramApiException e) {
            log.error("Error sending news update to Telegram: {}", e.getMessage());
        }
    }

    @Override
    public void sendError(String chatId, String errorMessage) {
        sendMessage(chatId, "❌ Error: " + errorMessage);
    }

    @Override
    public void sendStatus(String chatId, String status) {
        sendMessage(chatId, "📊 Status: " + status);
    }
}