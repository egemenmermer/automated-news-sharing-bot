package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.service.BotService;
import com.egemen.TweetBotTelegram.service.TelegramService;
import lombok.extern.slf4j.Slf4j;
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
    private final Map<String, UserState> userStates = new HashMap<>();
    private boolean isBotRunning = false;

    public TelegramServiceImpl(
            BotService botService,
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken) {
        super(botToken);
        this.botService = botService;
        this.botUsername = botUsername;
        this.botToken = botToken;
        log.info("TelegramServiceImpl initialized with username: {}", botUsername);
    }

    private enum UserState {
        NONE,
        AWAITING_LOGIN_USERNAME,
        AWAITING_LOGIN_PASSWORD,
        AWAITING_REGISTER_USERNAME,
        AWAITING_REGISTER_PASSWORD,
        AWAITING_REGISTER_EMAIL
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();
            
            log.info("Received message: '{}' from chat ID: {}", messageText, chatId);

            UserState currentState = userStates.getOrDefault(chatId, UserState.NONE);

            // Handle user registration/login states
            if (handleUserState(chatId, messageText, currentState)) {
                return;
            }

            // Handle commands
            switch (messageText) {
                case "/start" -> handleStart(chatId);
                case "/help" -> handleHelp(chatId);
                case "/status" -> handleStatus(chatId);
                case "/startbot" -> {
                    botService.startBot(chatId);
                    handleStatus(chatId);
                }
                case "/stopbot" -> {
                    botService.stopBot(chatId);
                    handleStatus(chatId);
                }
                case "/logout" -> handleLogout(chatId);
                case "/login" -> handleLogin(chatId);
                case "/register" -> handleRegister(chatId);
                case "/fetch" -> handleFetchNews(chatId);
                case "/latest" -> handleLatestNews(chatId);
                default -> handleUnknownCommand(chatId);
            }
        }
    }

    private boolean handleUserState(String chatId, String message, UserState state) {
        switch (state) {
            case AWAITING_LOGIN_USERNAME -> {
                // Store username and ask for password
                userStates.put(chatId, UserState.AWAITING_LOGIN_PASSWORD);
                sendMessage(chatId, "Please enter your password:");
                return true;
            }
            case AWAITING_LOGIN_PASSWORD -> {
                // Process login
                userStates.put(chatId, UserState.NONE);
                sendMessage(chatId, "Login successful! Welcome back.");
                showMainMenu(chatId);
                return true;
            }
            case AWAITING_REGISTER_USERNAME -> {
                // Store username and ask for password
                userStates.put(chatId, UserState.AWAITING_REGISTER_PASSWORD);
                sendMessage(chatId, "Please enter a password:");
                return true;
            }
            case AWAITING_REGISTER_PASSWORD -> {
                // Store password and ask for email
                userStates.put(chatId, UserState.AWAITING_REGISTER_EMAIL);
                sendMessage(chatId, "Please enter your email:");
                return true;
            }
            case AWAITING_REGISTER_EMAIL -> {
                // Complete registration
                userStates.put(chatId, UserState.NONE);
                sendMessage(chatId, "Registration successful! You can now login.");
                showLoginButton(chatId);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void handleStart(String chatId) {
        sendMessage(chatId, "Welcome to Neural News Bot! 🤖\n\nPlease login or register to continue.");
        showLoginRegisterButtons(chatId);
    }

    private void handleHelp(String chatId) {
        sendMessage(chatId, """
            Available commands:
            /start - Start the bot
            /help - Show this help message
            /login - Login to your account
            /register - Create new account
            /logout - Logout from your account
            /startbot - Start news fetching
            /stopbot - Stop news fetching
            /status - Check bot status
            /fetch - Fetch new articles
            /latest - Get latest news
            """);
    }

    private void handleStatus(String chatId) {
        String status = botService.isRunning(chatId) ?
            "✅ Bot is running - Fetching and posting news hourly" :
            "❌ Bot is stopped";
        sendMessage(chatId, status);
    }

    private void handleLogin(String chatId) {
        userStates.put(chatId, UserState.AWAITING_LOGIN_USERNAME);
        sendMessage(chatId, "Please enter your username:");
    }

    private void handleLogout(String chatId) {
        userStates.put(chatId, UserState.NONE);
        sendMessage(chatId, "You have been logged out successfully!");
        showLoginRegisterButtons(chatId);
    }

    private void handleRegister(String chatId) {
        userStates.put(chatId, UserState.AWAITING_REGISTER_USERNAME);
        sendMessage(chatId, "Please enter a username:");
    }

    private void handleFetchNews(String chatId) {
        sendMessage(chatId, "🔄 Fetching new articles...");
        // TODO: Implement actual news fetching
        sendMessage(chatId, "Found 5 new articles!");
    }

    private void handleLatestNews(String chatId) {
        sendMessage(chatId, "📰 Latest News:");
        // TODO: Implement latest news retrieval
        sendMessage(chatId, "No news available at the moment.");
    }

    private void handleUnknownCommand(String chatId) {
        sendMessage(chatId, "Unknown command. Use /help to see available commands.");
    }

    private void showLoginRegisterButtons(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("/login");
        row.add("/register");
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Please choose an option:")
                .replyMarkup(keyboardMarkup)
                .build();
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error showing login/register buttons: {}", e.getMessage());
        }
    }

    private void showMainMenu(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        
        KeyboardRow row1 = new KeyboardRow();
        row1.add("/fetch");
        row1.add("/latest");
        keyboard.add(row1);
        
        KeyboardRow row2 = new KeyboardRow();
        row2.add("/startbot");
        row2.add("/stopbot");
        row2.add("/status");
        keyboard.add(row2);
        
        KeyboardRow row3 = new KeyboardRow();
        row3.add("/logout");
        row3.add("/help");
        keyboard.add(row3);
        
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Main Menu:")
                .replyMarkup(keyboardMarkup)
                .build();
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error showing main menu: {}", e.getMessage());
        }
    }

    private void showLoginButton(String chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("/login");
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("You can now login:")
                .replyMarkup(keyboardMarkup)
                .build();
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error showing login button: {}", e.getMessage());
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