package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.BotConfig;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.enums.ConfigType;
import com.egemen.TweetBotTelegram.repository.BotConfigRepository;
import com.egemen.TweetBotTelegram.repository.PostLogsRepository;
import com.egemen.TweetBotTelegram.service.BotService;
import com.egemen.TweetBotTelegram.service.GeminiService;
import com.egemen.TweetBotTelegram.service.InstagramApiService;
import com.egemen.TweetBotTelegram.service.NewsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Stream;

@Slf4j
@Service
public class TelegramBotService extends TelegramLongPollingBot {
    private final NewsService newsService;
    private final InstagramApiService instagramApiService;
    private final GeminiService geminiService;
    private final BotService botService;
    private final BotConfigRepository botConfigurationRepository;
    private final PostLogsRepository postLogsRepository;
    private final TaskScheduler taskScheduler;

    @Value("${TELEGRAM_BOT_USERNAME}")
    private String botUsername;

    @Value("${TELEGRAM_BOT_TOKEN}")
    private String botToken;

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<Long, String> userStates = new ConcurrentHashMap<>();
    private final Map<Long, Bot> userBots = new ConcurrentHashMap<>();

    public TelegramBotService(NewsService newsService,
                             InstagramApiService instagramApiService,
                             GeminiService geminiService,
                             BotService botService,
                             BotConfigRepository botConfigurationRepository,
                             PostLogsRepository postLogsRepository,
                             TaskScheduler taskScheduler) {
        super(new DefaultBotOptions());
        this.newsService = newsService;
        this.instagramApiService = instagramApiService;
        this.geminiService = geminiService;
        this.botService = botService;
        this.botConfigurationRepository = botConfigurationRepository;
        this.postLogsRepository = postLogsRepository;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        super.onUpdatesReceived(updates);
    }

    @Override
    public void onClosing() {
        super.onClosing();
    }

    @EventListener({ContextRefreshedEvent.class})
    public void init() throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            telegramBotsApi.registerBot(this);
            log.info("Telegram bot has been registered successfully!");
        } catch (TelegramApiException e) {
            log.error("Error occurred while registering bot: " + e.getMessage());
        }
    }

    private void sendWelcomeMessage(long chatId) {
        String welcomeText = "Welcome to Neural News Bot! \n\n" +
                "I can help you manage your news posting automation.\n\n" +
                "Please select an option from the menu below:";

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(welcomeText);
        message.setReplyMarkup(getMainMenuKeyboard());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending welcome message: ", e);
        }
    }

    private InlineKeyboardMarkup getMainMenuKeyboard() {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // First row
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("🔄 Fetch News", "fetch_news"));
        row1.add(createButton("📱 Post News", "post_news"));
        rowsInline.add(row1);

        // Second row
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("⚙️ Configure", "configure"));
        row2.add(createButton("📊 Status", "status"));
        rowsInline.add(row2);

        // Third row
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createButton("❓ Help", "help"));
        rowsInline.add(row3);

        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }

    private InlineKeyboardMarkup getConfigMenuKeyboard() {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // First row
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("📰 Set Topic", "set_topic"));
        row1.add(createButton("🔢 Set Fetch Amount", "set_fetch_amount"));
        rowsInline.add(row1);

        // Second row
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("⏰ Set Schedule", "set_schedule"));
        rowsInline.add(row2);

        // Third row
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createButton("◀️ Back to Main Menu", "main_menu"));
        rowsInline.add(row3);

        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();

                if (messageText.startsWith("/start")) {
                    sendWelcomeMessage(chatId);
                } else if (messageText.startsWith("/help")) {
                    sendHelpMessage(chatId);
                } else if (messageText.startsWith("/status")) {
                    sendStatusMessage(chatId);
                }
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("Error in onUpdateReceived: ", e);
        }
    }

    private void sendHelpMessage(long chatId) {
        String helpText = "Neural News Bot Help \n\n" +
                "Commands:\n" +
                "/start - Start the bot\n" +
                "/help - Show this help message\n" +
                "/status - Check bot status\n\n" +
                "The bot will automatically fetch and post news based on your configuration.";

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(helpText);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending help message: ", e);
        }
    }

    private void sendStatusMessage(long chatId) {
        try {
            List<Bot> bots = botService.listBots();
            StringBuilder statusText = new StringBuilder("Bot Status Report \n\n");

            for (Bot bot : bots) {
                statusText.append("Bot: ").append(bot.getName()).append("\n");
                statusText.append("Status: Active \n");
                statusText.append("Last check: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
            }

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(statusText.toString());
            execute(message);
        } catch (Exception e) {
            log.error("Error sending status message: ", e);
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId);
            errorMessage.setText("Error getting bot status. Please try again later.");
            try {
                execute(errorMessage);
            } catch (TelegramApiException ex) {
                log.error("Error sending error message: ", ex);
            }
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();

        try {
            switch (callbackData) {
                case "fetch_news":
                    handleFetchNews(chatId);
                    break;
                case "post_news":
                    handlePostNews(chatId);
                    break;
                case "configure":
                    showConfigMenu(chatId);
                    break;
                case "status":
                    sendStatusMessage(chatId);
                    break;
                case "help":
                    sendHelpMessage(chatId);
                    break;
                case "main_menu":
                    sendWelcomeMessage(chatId);
                    break;
                case "set_topic":
                    handleSetTopic(chatId);
                    break;
                case "set_fetch_amount":
                    handleSetFetchAmount(chatId);
                    break;
                case "set_schedule":
                    handleSetSchedule(chatId);
                    break;
                default:
                    sendMessage(chatId, "Unknown command. Please try again.");
            }
        } catch (Exception e) {
            log.error("Error handling callback query: ", e);
            sendMessage(chatId, "Error processing your request. Please try again.");
        }
    }

    private void showConfigMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("⚙️ Configuration Menu\n\nPlease select what you want to configure:");
        message.setReplyMarkup(getConfigMenuKeyboard());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error showing config menu: ", e);
        }
    }

    private void handleFetchNews(long chatId) {
        try {
            Bot bot = botService.listBots().get(0); // Get the first bot for now
            List<News> fetchedNews = newsService.fetchAndSaveNews(bot.getId(), false);
            String message = fetchedNews.isEmpty() ? 
                "No new articles found." :
                "Successfully fetched " + fetchedNews.size() + " new articles!";
            sendMessage(chatId, message);
        } catch (Exception e) {
            log.error("Error fetching news: ", e);
            sendMessage(chatId, "Error fetching news. Please try again later.");
        }
    }

    private void handlePostNews(long chatId) {
        try {
            Bot bot = botService.listBots().get(0); // Get the first bot for now
            // TODO: Implement news posting logic
            sendMessage(chatId, "News posting feature is coming soon!");
        } catch (Exception e) {
            log.error("Error posting news: ", e);
            sendMessage(chatId, "Error posting news. Please try again later.");
        }
    }

    private void handleSetTopic(long chatId) {
        userStates.put(chatId, "WAITING_TOPIC");
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Please enter the topic for news (e.g., technology, business, sports):");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error in handleSetTopic: ", e);
        }
    }

    private void handleSetFetchAmount(long chatId) {
        userStates.put(chatId, "WAITING_FETCH_AMOUNT");
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Please enter the number of articles to fetch (1-100):");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error in handleSetFetchAmount: ", e);
        }
    }

    private void handleSetSchedule(long chatId) {
        userStates.put(chatId, "WAITING_SCHEDULE");
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Please enter the schedule in minutes for fetching news (e.g., 30 for every 30 minutes):");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error in handleSetSchedule: ", e);
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message: ", e);
        }
    }
}