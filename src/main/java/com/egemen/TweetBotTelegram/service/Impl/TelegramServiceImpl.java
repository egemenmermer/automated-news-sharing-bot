package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.dto.NewsArticleDTO;
import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.service.BotService;
import com.egemen.TweetBotTelegram.service.TelegramService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramBot;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TelegramServiceImpl extends TelegramLongPollingBot implements TelegramService {

    private final BotService botService;

    private final String botUsername;
    private final String botToken;
    private boolean isBotRunning = false;

    public TelegramServiceImpl(@Lazy BotService botService,
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken) {
        super(botToken);
        this.botService = botService;
        this.botUsername = botUsername;
        this.botToken = botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String chatId = update.getMessage().getChatId().toString();
        String message = update.getMessage().getText();

        handleCommand(chatId, message);
    }

    private void handleCommand(String chatId, String message) {
        switch (message.toLowerCase()) {
            case "/start" -> {
                sendMessage(chatId, "Welcome to Neural News Bot! 🤖\n\n" +
                        "Available commands:\n" +
                        "/fetch - Fetch latest news\n" +
                        "/latest - Show latest news\n" +
                        "/status - Show bot status\n" +
                        "/startbot - Start automated posting (every hour)\n" +
                        "/stopbot - Stop automated posting\n" +
                        "/pending - Show pending posts\n" +
                        "/publish - Publish pending posts now");
                sendKeyboard(chatId);
            }
            case "/fetch" -> handleFetchNews(chatId);
            case "/latest" -> handleLatestNews(chatId);
            case "/status" -> handleStatus(chatId);
            case "/startbot" -> handleStartBot(chatId);
            case "/stopbot" -> handleStopBot(chatId);
            case "/pending" -> handlePendingPosts(chatId);
            case "/publish" -> handlePublishPosts(chatId);
            default -> sendMessage(chatId, "Unknown command. Type /start for available commands.");
        }
    }

    private void sendKeyboard(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        KeyboardRow row1 = new KeyboardRow();
        row1.add("/fetch");
        row1.add("/latest");
        
        KeyboardRow row2 = new KeyboardRow();
        row2.add("/status");
        row2.add("/pending");
        
        KeyboardRow row3 = new KeyboardRow();
        row3.add("/startbot");
        row3.add("/stopbot");
        row3.add("/publish");
        
        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Keyboard enabled. Choose a command:")
                .replyMarkup(keyboardMarkup)
                .build();
                
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending keyboard: {}", e.getMessage());
        }
    }

    private void handleStatus(String chatId) {
        try {
            boolean isRunning = botService.isRunning(chatId);
            int pendingPosts = botService.getPendingPostsCount(chatId);
            
            String status = String.format("""
                🤖 Bot Status:
                
                Automated Posting: %s
                Pending Posts: %d
                
                Last Update: %s
                """, 
                isRunning ? "✅ Running" : "❌ Stopped",
                pendingPosts,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                
            sendMessage(chatId, status);
        } catch (Exception e) {
            sendError(chatId, "Error getting bot status");
        }
    }

    private void handleStartBot(String chatId) {
        try {
            botService.startBot(chatId);
            sendMessage(chatId, "✅ Bot started! Will post news automatically every hour.");
        } catch (Exception e) {
            sendError(chatId, "Failed to start bot");
        }
    }

    private void handleStopBot(String chatId) {
        try {
            botService.stopBot(chatId);
            sendMessage(chatId, "🛑 Bot stopped! Automated posting disabled.");
        } catch (Exception e) {
            sendError(chatId, "Failed to stop bot");
        }
    }

    private void handlePendingPosts(String chatId) {
        try {
            List<InstagramPost> pendingPosts = botService.getPendingPosts(chatId);
            if (pendingPosts.isEmpty()) {
                sendMessage(chatId, "No pending posts.");
                return;
            }

            StringBuilder message = new StringBuilder("📝 Pending Posts:\n\n");
            for (InstagramPost post : pendingPosts) {
                message.append(String.format("• %s\n", post.getNews().getTitle()));
            }
            message.append("\nUse /publish to publish these posts now.");
            
            sendMessage(chatId, message.toString());
        } catch (Exception e) {
            sendError(chatId, "Error getting pending posts");
        }
    }

    private void handlePublishPosts(String chatId) {
        try {
            sendMessage(chatId, "🔄 Publishing pending posts...");
            botService.publishPendingPosts(chatId);
            sendMessage(chatId, "✅ Posts published successfully!");
        } catch (Exception e) {
            sendError(chatId, "Error publishing posts");
        }
    }

    private void handleFetchNews(String chatId) {
        try {
            sendMessage(chatId, "🔄 Fetching new articles...");
            int count = botService.fetchLatestNews(chatId);
            sendMessage(chatId, String.format("✅ Found %d new articles!", count));
        } catch (Exception e) {
            log.error("Error fetching news: {}", e.getMessage());
            sendError(chatId, "Failed to fetch news. Please try again later.");
        }
    }

    private void handleLatestNews(String chatId) {
        try {
            List<NewsArticleDTO> latestNews = botService.getLatestNews(chatId, 5);
            if (latestNews.isEmpty()) {
                sendMessage(chatId, "No news available at the moment.");
                return;
            }

            StringBuilder message = new StringBuilder("📰 Latest News:\n\n");
            for (NewsArticleDTO news : latestNews) {
                message.append(String.format("• <b>%s</b>\n%s\n%s\n\n", 
                    news.getTitle(), 
                    news.getDescription(),
                    news.getUrl()));
            }
            sendMessage(chatId, message.toString());
        } catch (Exception e) {
            log.error("Error getting latest news: {}", e.getMessage());
            sendError(chatId, "Failed to retrieve latest news. Please try again later.");
        }
    }

    @Override
    public void sendMessage(String chatId, String text) {
        try {
            SendMessage message = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .parseMode("HTML")
                    .build();
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message to Telegram: {}", e.getMessage());
        }
    }

    @Override
    public void sendNewsUpdate(String chatId, String title, String content, String imageUrl) {
        try {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                SendPhoto sendPhoto = SendPhoto.builder()
                        .chatId(chatId)
                        .photo(new InputFile(imageUrl))
                        .caption(String.format("<b>%s</b>\n\n%s", title, content))
                        .parseMode("HTML")
                        .build();
                execute(sendPhoto);
            } else {
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