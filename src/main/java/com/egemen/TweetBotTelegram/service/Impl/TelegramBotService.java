package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.BotConfig;
import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.enums.ConfigType;
import com.egemen.TweetBotTelegram.enums.NewsStatus;
import com.egemen.TweetBotTelegram.enums.PostStatus;
import com.egemen.TweetBotTelegram.repository.BotConfigRepository;
import com.egemen.TweetBotTelegram.repository.PostLogsRepository;
import com.egemen.TweetBotTelegram.scheduler.NewsProcessingScheduler;
import com.egemen.TweetBotTelegram.service.BotService;
import com.egemen.TweetBotTelegram.service.GeminiService;
import com.egemen.TweetBotTelegram.service.InstagramApiService;
import com.egemen.TweetBotTelegram.service.NewsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final NewsProcessingScheduler newsProcessingScheduler;
    private final String botUsername;
    private final String botToken;

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<Long, String> userStates = new ConcurrentHashMap<>();
    private final Map<Long, Bot> userBots = new ConcurrentHashMap<>();

    @Autowired
    public TelegramBotService(NewsService newsService,
                             InstagramApiService instagramApiService,
                             GeminiService geminiService,
                             BotService botService,
                             BotConfigRepository botConfigurationRepository,
                             PostLogsRepository postLogsRepository,
                             TaskScheduler taskScheduler,
                             NewsProcessingScheduler newsProcessingScheduler,
                             String telegramBotUsername,
                             String telegramBotToken) {
        super(new DefaultBotOptions());
        this.newsService = newsService;
        this.instagramApiService = instagramApiService;
        this.geminiService = geminiService;
        this.botService = botService;
        this.botConfigurationRepository = botConfigurationRepository;
        this.postLogsRepository = postLogsRepository;
        this.taskScheduler = taskScheduler;
        this.newsProcessingScheduler = newsProcessingScheduler;
        this.botUsername = telegramBotUsername;
        this.botToken = telegramBotToken;
        log.info("TelegramBotService initialized with bot username: {}", botUsername);
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

    private void handleStartProcessing(long chatId) {
        try {
            newsProcessingScheduler.enableScheduler();
            sendMessage(chatId, "News processing scheduler has been enabled.");
        } catch (Exception e) {
            log.error("Error enabling news processing scheduler: {}", e.getMessage());
            sendMessage(chatId, "Failed to enable news processing scheduler.");
        }
    }

    private void handleStopProcessing(long chatId) {
        try {
            newsProcessingScheduler.disableScheduler();
            sendMessage(chatId, "News processing scheduler has been disabled.");
        } catch (Exception e) {
            log.error("Error disabling news processing scheduler: {}", e.getMessage());
            sendMessage(chatId, "Failed to disable news processing scheduler.");
        }
    }

    private void handleProcessingStatus(long chatId) {
        boolean isEnabled = newsProcessingScheduler.isSchedulerEnabled();
        String status = isEnabled ? "enabled" : "disabled";
        sendMessage(chatId, "News processing scheduler is currently " + status + ".");
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
            log.info("Bot initialized and ready for manual operation");
        } catch (TelegramApiException e) {
            log.error("Error occurred while registering bot: " + e.getMessage());
        }
    }
    
    private void initializeHourlyPostingScheduler() {
        // Schedule a task to run every hour to post one article
        Runnable postTask = () -> {
            try {
                log.info("Running hourly posting task");
                postOneArticle();
            } catch (Exception e) {
                log.error("Error in hourly posting task: {}", e.getMessage());
            }
        };
        
        // Schedule the task to run every hour using a fixed delay
        taskScheduler.scheduleWithFixedDelay(postTask, 3600000);
        log.info("Hourly posting scheduler initialized");
    }
    
    private void handleScheduleBot(long chatId) {
        try {
            List<Bot> bots = botService.listBots();
            if (bots.isEmpty()) {
                sendMessage(chatId, "No bots configured. Please add a bot first.");
                return;
            }
            
            InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
            
            for (Bot bot : bots) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(createButton(bot.getName(), "schedule_bot_" + bot.getId()));
                rowsInline.add(row);
            }
            
            markupInline.setKeyboard(rowsInline);
            
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Select a bot to schedule:");
            message.setReplyMarkup(markupInline);
            
            execute(message);
        } catch (Exception e) {
            log.error("Error showing schedule bot menu: ", e);
            sendMessage(chatId, "Error retrieving bots. Please try again.");
        }
    }
    
    private void handleStopSchedule(long chatId) {
        try {
            List<Bot> bots = botService.listBots();
            if (bots.isEmpty()) {
                sendMessage(chatId, "No bots configured. Please add a bot first.");
                return;
            }
            
            InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
            
            for (Bot bot : bots) {
                if (scheduledTasks.containsKey(bot.getId())) {
                    List<InlineKeyboardButton> row = new ArrayList<>();
                    row.add(createButton(bot.getName(), "stop_schedule_" + bot.getId()));
                    rowsInline.add(row);
                }
            }
            
            if (rowsInline.isEmpty()) {
                sendMessage(chatId, "No scheduled bots found.");
                return;
            }
            
            markupInline.setKeyboard(rowsInline);
            
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Select a bot to stop scheduling:");
            message.setReplyMarkup(markupInline);
            
            execute(message);
        } catch (Exception e) {
            log.error("Error showing stop schedule menu: ", e);
            sendMessage(chatId, "Error retrieving scheduled bots. Please try again.");
        }
    }
    
    private void stopBotScheduler(long chatId, long botId) {
        ScheduledFuture<?> task = scheduledTasks.get(botId);
        if (task != null) {
            task.cancel(false);
            scheduledTasks.remove(botId);
            sendMessage(chatId, "Scheduling stopped for bot ID: " + botId);
            log.info("Stopped scheduler for bot ID {}", botId);
        } else {
            sendMessage(chatId, "No active schedule found for this bot.");
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

        // First row - Bot Management
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createButton("➕ Add Bot", "add_bot"));
        row1.add(createButton("🛑 Stop Bot", "stop_bot"));
        row1.add(createButton("❌ Delete Bot", "delete_bot"));
        rowsInline.add(row1);

        // Second row - News Operations
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createButton("🔄 Fetch News", "fetch_news"));
        row2.add(createButton("📱 Post News", "post_news"));
        rowsInline.add(row2);

        // Third row - Scheduling
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createButton("⏰ Schedule", "schedule"));
        row3.add(createButton("🚫 Stop Schedule", "stop_schedule"));
        rowsInline.add(row3);

        // Fourth row - Configuration and Status
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        row4.add(createButton("⚙️ Configure", "configure"));
        row4.add(createButton("📊 Status", "status"));
        rowsInline.add(row4);

        // Fifth row - Help
        List<InlineKeyboardButton> row5 = new ArrayList<>();
        row5.add(createButton("❓ Help", "help"));
        rowsInline.add(row5);

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
                } else if (messageText.equals("/start_processing")) {
                    handleStartProcessing(chatId);
                } else if (messageText.equals("/stop_processing")) {
                    handleStopProcessing(chatId);
                } else if (messageText.equals("/processing_status")) {
                    handleProcessingStatus(chatId);
                } else {
                    // Handle user input based on current state
                    String currentState = userStates.getOrDefault(chatId, "");
                    
                    switch (currentState) {
                        case "WAITING_BOT_NAME":
                            createBot(chatId, messageText.trim());
                            break;
                        case "WAITING_TOPIC":
                            saveBotConfig(chatId, "topic", messageText.trim());
                            break;
                        case "WAITING_FETCH_AMOUNT":
                            try {
                                int amount = Integer.parseInt(messageText.trim());
                                if (amount > 0 && amount <= 100) {
                                    saveBotConfig(chatId, "fetch_amount", String.valueOf(amount));
                                } else {
                                    sendMessage(chatId, "Please enter a valid number between 1 and 100.");
                                }
                            } catch (NumberFormatException e) {
                                sendMessage(chatId, "Please enter a valid number.");
                            }
                            break;
                        case "WAITING_SCHEDULE":
                            try {
                                int minutes = Integer.parseInt(messageText.trim());
                                if (minutes > 0) {
                                    saveBotConfig(chatId, "schedule_minutes", String.valueOf(minutes));
                                } else {
                                    sendMessage(chatId, "Please enter a valid number greater than 0.");
                                }
                            } catch (NumberFormatException e) {
                                sendMessage(chatId, "Please enter a valid number.");
                            }
                            break;
                    }
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
                "Bot Management:\n" +
                "➕ Add Bot - Create a new news bot\n" +
                "🛑 Stop Bot - Pause a running bot\n" +
                "❌ Delete Bot - Remove a bot\n\n" +
                "News Operations:\n" +
                "🔄 Fetch News - Manually fetch news\n" +
                "📱 Post News - Post pending news\n\n" +
                "The system will automatically post one article per hour when running.";

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
            // Handle bot-specific actions (stop_bot_ID, delete_bot_ID, schedule_bot_ID, stop_schedule_ID)
            if (callbackData.startsWith("stop_bot_")) {
                String botIdStr = callbackData.substring("stop_bot_".length());
                try {
                    Long botId = Long.parseLong(botIdStr);
                    stopBotById(chatId, botId);
                    return;
                } catch (NumberFormatException e) {
                    log.error("Invalid bot ID format: {}", botIdStr);
                }
            } else if (callbackData.startsWith("delete_bot_")) {
                String botIdStr = callbackData.substring("delete_bot_".length());
                try {
                    Long botId = Long.parseLong(botIdStr);
                    deleteBotById(chatId, botId);
                    return;
                } catch (NumberFormatException e) {
                    log.error("Invalid bot ID format: {}", botIdStr);
                }
            } else if (callbackData.startsWith("schedule_bot_")) {
                String botIdStr = callbackData.substring("schedule_bot_".length());
                try {
                    Long botId = Long.parseLong(botIdStr);
                    handleSetScheduleForBot(chatId, botId);
                    return;
                } catch (NumberFormatException e) {
                    log.error("Invalid bot ID format: {}", botIdStr);
                }
            } else if (callbackData.startsWith("stop_schedule_")) {
                String botIdStr = callbackData.substring("stop_schedule_".length());
                try {
                    Long botId = Long.parseLong(botIdStr);
                    stopBotScheduler(chatId, botId);
                    return;
                } catch (NumberFormatException e) {
                    log.error("Invalid bot ID format: {}", botIdStr);
                }
            }
            
            // Handle standard menu actions
            switch (callbackData) {
                case "add_bot":
                    handleAddBot(chatId);
                    break;
                case "stop_bot":
                    handleStopBot(chatId);
                    break;
                case "delete_bot":
                    handleDeleteBot(chatId);
                    break;
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
                case "schedule":
                    handleScheduleBot(chatId);
                    break;
                case "stop_schedule":
                    handleStopSchedule(chatId);
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
        try {
            // Get all bots first
            List<Bot> bots = botService.listBots();
            if (bots.isEmpty()) {
                sendMessage(chatId, "No bots found. Please create a bot first.");
                showMainMenu(chatId);
                return;
            }

            // If there's no selected bot for this user, select the first one
            if (!userBots.containsKey(chatId)) {
                userBots.put(chatId, bots.get(0));
            }

            Bot currentBot = userBots.get(chatId);
            
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            // Topic configuration button
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton topicButton = new InlineKeyboardButton();
            topicButton.setText("📰 Set Topic");
            topicButton.setCallbackData("set_topic");
            row1.add(topicButton);
            keyboard.add(row1);

            // Fetch amount configuration button
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton fetchAmountButton = new InlineKeyboardButton();
            fetchAmountButton.setText("🔢 Set Fetch Amount");
            fetchAmountButton.setCallbackData("set_fetch_amount");
            row2.add(fetchAmountButton);
            keyboard.add(row2);

            // Schedule configuration button
            List<InlineKeyboardButton> row3 = new ArrayList<>();
            InlineKeyboardButton scheduleButton = new InlineKeyboardButton();
            scheduleButton.setText("⏰ Set Schedule");
            scheduleButton.setCallbackData("set_schedule");
            row3.add(scheduleButton);
            keyboard.add(row3);

            // Back button
            List<InlineKeyboardButton> row4 = new ArrayList<>();
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("◀️ Back to Main Menu");
            backButton.setCallbackData("back_to_main");
            row4.add(backButton);
            keyboard.add(row4);

            markup.setKeyboard(keyboard);

            String message = String.format("⚙️ Configuration Menu for bot: %s\n\n" +
                    "Current settings:\n" +
                    "• Topic: %s\n" +
                    "• Fetch Amount: %s\n" +
                    "• Schedule: Every %s minutes",
                    currentBot.getName(),
                    getBotConfig(currentBot, ConfigType.TOPIC, "not set"),
                    getBotConfig(currentBot, ConfigType.FETCH_AMOUNT, "10"),
                    getBotConfig(currentBot, ConfigType.SCHEDULE_MINUTES, "30"));

            SendMessage configMessage = new SendMessage();
            configMessage.setChatId(String.valueOf(chatId));
            configMessage.setText(message);
            configMessage.setReplyMarkup(markup);

            execute(configMessage);
        } catch (Exception e) {
            log.error("Error showing config menu: {}", e.getMessage(), e);
            sendMessage(chatId, "Error showing configuration menu. Please try again.");
        }
    }

    private String getBotConfig(Bot bot, ConfigType configType, String defaultValue) {
        Optional<BotConfig> config = botConfigurationRepository.findByBotAndConfigType(bot, configType);
        return config.map(BotConfig::getConfigValue).orElse(defaultValue);
    }

    private void handleCallbackQuery(long chatId, String callbackData) {
        try {
            switch (callbackData) {
                case "create_bot":
                    userStates.put(chatId, "WAITING_BOT_NAME");
                    sendMessage(chatId, "Please enter a name for your new bot:");
                    break;
                case "configure_bot":
                    showConfigMenu(chatId);
                    break;
                case "post_news":
                    handlePostNews(chatId);
                    break;
                case "back_to_main":
                    userStates.remove(chatId);
                    showMainMenu(chatId);
                    break;
                case "set_topic":
                case "set_fetch_amount":
                case "set_schedule":
                    handleConfigOption(chatId, callbackData);
                    break;
                default:
                    sendMessage(chatId, "Invalid option selected.");
                    break;
            }
        } catch (Exception e) {
            log.error("Error handling callback query: {}", e.getMessage(), e);
            sendMessage(chatId, "Error processing your request. Please try again.");
        }
    }

    private void handleConfigOption(long chatId, String option) {
        Bot currentBot = userBots.get(chatId);
        if (currentBot == null) {
            sendMessage(chatId, "⚠️ No bot selected. Please create or select a bot first.");
            showMainMenu(chatId);
            return;
        }

        switch (option) {
            case "set_topic":
                userStates.put(chatId, "waiting_for_topic");
                sendMessage(chatId, "Please enter the topic for news fetching (e.g., technology, sports, business):");
                break;
            case "set_fetch_amount":
                userStates.put(chatId, "waiting_for_fetch_amount");
                sendMessage(chatId, "Please enter the number of articles to fetch (1-50):");
                break;
            case "set_schedule":
                userStates.put(chatId, "waiting_for_schedule");
                sendMessage(chatId, "Please enter the schedule interval in minutes (e.g., 30 for every 30 minutes):");
                break;
        }
    }

    private void handleFetchNews(long chatId) {
        try {
            List<Bot> bots = botService.listBots();
            if (bots.isEmpty()) {
                sendMessage(chatId, "No bots configured. Please add a bot first.");
                return;
            }

            StringBuilder resultMessage = new StringBuilder("Fetch results:\n\n");
            for (Bot bot : bots) {
                List<News> fetchedNews = newsService.fetchAndSaveNews(bot.getId(), false);
                resultMessage.append(String.format("Bot '%s': %d articles fetched\n",
                    bot.getName(), fetchedNews.size()));
            }
            sendMessage(chatId, resultMessage.toString());
        } catch (Exception e) {
            log.error("Error fetching news: ", e);
            sendMessage(chatId, "Error fetching news. Please try again later.");
        }
    }

    @Transactional(readOnly = false)
    private void handlePostNews(long chatId) {
        try {
            List<Bot> bots = botService.listBots();
            if (bots.isEmpty()) {
                sendMessage(chatId, "No bots configured. Please add a bot first.");
                return;
            }

            Bot bot = bots.get(0);
            List<News> pendingNews = newsService.getPendingNews(bot, 1);
            
            if (pendingNews.isEmpty()) {
                sendMessage(chatId, "No pending news articles found for posting.");
                return;
            }

            News news = pendingNews.get(0);
            
            // Create Instagram post with all necessary information
            InstagramPost instagramPost = new InstagramPost();
            instagramPost.setBot(bot);
            instagramPost.setNews(news);
            instagramPost.setTitle(news.getTitle());
            instagramPost.setContent(news.getContent());
            instagramPost.setUrl(news.getUrl());
            instagramPost.setImageUrl(news.getImageUrl());
            instagramPost.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            instagramPost.setPostStatus(PostStatus.PENDING);

            // Try to post to Instagram
            boolean posted = instagramApiService.createPost(instagramPost);

            if (posted) {
                newsService.updateNewsStatus(news.getId(), NewsStatus.POSTED);
                sendMessage(chatId, "Successfully posted to Instagram: " + news.getTitle());
            } else {
                newsService.updateNewsStatus(news.getId(), NewsStatus.FAILED);
                sendMessage(chatId, "Failed to post to Instagram: " + news.getTitle());
            }
            
        } catch (Exception e) {
            log.error("Error in handlePostNews: {}", e.getMessage(), e);
            sendMessage(chatId, "Error processing article: " + e.getMessage());
        }
    }
    
    /**
     * Posts one article from any of the active bots
     * @return true if an article was posted successfully, false otherwise
     */
    private boolean postOneArticle() {
        try {
            List<Bot> bots = botService.listBots();
            if (bots.isEmpty()) {
                log.info("No bots configured for posting");
                return false;
            }
            
            // Try each bot until we find one with pending news
            for (Bot bot : bots) {
                List<News> pendingNews = newsService.getPendingNews(bot.getId());
                
                if (!pendingNews.isEmpty()) {
                    // Take just the first article
                    News news = pendingNews.get(0);
                    
                    try {
                        // Generate image and caption using Gemini service
                        String imageUrl = geminiService.generateImageForNews(news);
                        if (imageUrl == null || imageUrl.isEmpty()) {
                            log.error("Failed to generate image for news article: {}", news.getTitle());
                            continue;
                        }

                        String caption = geminiService.generateInstagramCaption(news);
                        if (caption == null || caption.isEmpty()) {
                            log.error("Failed to generate caption for news article: {}", news.getTitle());
                            continue;
                        }

                        // Create Instagram post with all required fields
                        InstagramPost instagramPost = new InstagramPost();
                        instagramPost.setBot(bot);
                        instagramPost.setNews(news);
                        instagramPost.setImageUrl(imageUrl);
                        instagramPost.setCaption(caption);
                        instagramPost.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                        instagramPost.setPostStatus(PostStatus.PENDING);
                        instagramPost.setImagePrompt(imageUrl);
                        instagramPost.setRetryCount(0);

                        // Post to Instagram
                        boolean posted = instagramApiService.createPost(instagramPost);
                        
                        if (posted) {
                            log.info("Successfully posted article from bot {}: {}", bot.getName(), news.getTitle());
                            return true;
                        } else {
                            log.error("Failed to post article from bot {}: {}", bot.getName(), news.getTitle());
                        }
                    } catch (Exception e) {
                        log.error("Error processing article from bot {}: {}", bot.getName(), e.getMessage());
                        continue;
                    }
                }
            }
            
            log.info("No pending news articles found for posting");
            return false;
        } catch (Exception e) {
            log.error("Error in postOneArticle: {}", e.getMessage(), e);
            return false;
        }
    }

    private void handleSetTopic(long chatId) {
        Bot bot = userBots.get(chatId);
        if (bot == null) {
            sendMessage(chatId, "Please create a bot first.");
            return;
        }
        userStates.put(chatId, "WAITING_TOPIC");
        sendMessage(chatId, "Please enter the topic for news articles (e.g., 'technology', 'sports', etc.):");
    }

    private void handleSetFetchAmount(long chatId) {
        Bot bot = userBots.get(chatId);
        if (bot == null) {
            sendMessage(chatId, "Please create a bot first.");
            return;
        }
        userStates.put(chatId, "WAITING_FETCH_AMOUNT");
        sendMessage(chatId, "Please enter the number of articles to fetch (1-100):");
    }

    private void handleSetSchedule(long chatId) {
        Bot bot = userBots.get(chatId);
        if (bot == null) {
            sendMessage(chatId, "Please create a bot first.");
            return;
        }
        userStates.put(chatId, "WAITING_SCHEDULE");
        sendMessage(chatId, "Please enter the schedule interval in minutes (e.g., 60 for hourly):\n\nEnter 0 to disable automatic scheduling.");
    }

    private void handleSetScheduleForBot(long chatId, long botId) {
        try {
            Bot bot = botService.getBotById(botId);
            if (bot == null) {
                sendMessage(chatId, "Bot not found. Please try again.");
                return;
            }
            
            userBots.put(chatId, bot);
            userStates.put(chatId, "WAITING_SCHEDULE");
            
            sendMessage(chatId, "You selected bot: " + bot.getName() + 
                    "\n\nPlease enter the schedule interval in minutes (e.g., 30 for every 30 minutes)." +
                    "\n\nEnter 0 to disable automatic scheduling.");
            
        } catch (Exception e) {
            log.error("Error setting schedule for bot: ", e);
            sendMessage(chatId, "Error setting schedule. Please try again.");
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }
    
    // Bot management methods
    private void handleAddBot(long chatId) {
        userStates.put(chatId, "WAITING_BOT_NAME");
        sendMessage(chatId, "Please enter a name for the new bot:");
    }
    
    private void handleStopBot(long chatId) {
        List<Bot> bots = botService.listBots();
        if (bots.isEmpty()) {
            sendMessage(chatId, "No bots available to stop.");
            return;
        }
        
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        
        for (Bot bot : bots) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(createButton(bot.getName(), "stop_bot_" + bot.getId()));
            rowsInline.add(row);
        }
        
        markupInline.setKeyboard(rowsInline);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Select a bot to stop:");
        message.setReplyMarkup(markupInline);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error showing stop bot menu: ", e);
        }
    }
    
    private void handleDeleteBot(long chatId) {
        List<Bot> bots = botService.listBots();
        if (bots.isEmpty()) {
            sendMessage(chatId, "No bots available to delete.");
            return;
        }
        
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        
        for (Bot bot : bots) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(createButton(bot.getName(), "delete_bot_" + bot.getId()));
            rowsInline.add(row);
        }
        
        markupInline.setKeyboard(rowsInline);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("⚠️ Select a bot to delete (this cannot be undone):");
        message.setReplyMarkup(markupInline);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error showing delete bot menu: ", e);
        }
    }
    
    @org.springframework.transaction.annotation.Transactional(readOnly = false)
    private void startBotScheduler(Bot bot, int scheduleMinutes) {
        // Cancel any existing scheduled task for this bot
        ScheduledFuture<?> existingTask = scheduledTasks.get(bot.getId());
        if (existingTask != null) {
            existingTask.cancel(false);
            scheduledTasks.remove(bot.getId());
            log.info("Cancelled existing schedule for bot {}", bot.getName());
        }
        
        if (scheduleMinutes <= 0) {
            log.info("No schedule set for bot {}", bot.getName());
            return;
        }
        
        // Create a new task for fetching news periodically
        Runnable fetchTask = () -> {
            try {
                log.info("Running scheduled fetch for bot {}", bot.getName());
                List<News> fetchedNews = newsService.fetchAndSaveNews(bot.getId(), false);
                log.info("Fetched {} new articles for bot {}", fetchedNews.size(), bot.getName());
                
                // Update bot's last run timestamp
                bot.setLastRun(new Timestamp(System.currentTimeMillis()));
                botService.saveBot(bot);
            } catch (Exception e) {
                log.error("Error in scheduled fetch for bot {}: {}", bot.getName(), e.getMessage());
            }
        };

        // Schedule the task
        ScheduledFuture<?> scheduledTask = taskScheduler.scheduleAtFixedRate(
            fetchTask, scheduleMinutes * 60 * 1000);
        
        // Store the scheduled task
        scheduledTasks.put(bot.getId(), scheduledTask);
        log.info("Started scheduler for bot {} with interval {} minutes", bot.getName(), scheduleMinutes);
    }
    
    private void stopBotScheduler(Long botId) {
        ScheduledFuture<?> existingTask = scheduledTasks.get(botId);
        if (existingTask != null) {
            existingTask.cancel(false);
            scheduledTasks.remove(botId);
            log.info("Stopped scheduler for bot ID {}", botId);
        }
    }

    private void createBot(long chatId, String botName) {
        try {
            log.info("Creating new bot with name: {}", botName);
            
            // Create and save the bot
            Bot bot = new Bot();
            bot.setName(botName);
            bot.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            bot.setLastRun(new Timestamp(System.currentTimeMillis()));
            bot.setFetchTime("0 */30 * * * *"); // Default to every 30 minutes
            bot.setPostTime("0 0 */1 * * *");   // Default to every hour
            
            Bot savedBot = botService.saveBot(bot);
            log.info("Bot saved with ID: {}", savedBot.getId());
            
            // Create default configurations
            BotConfig topicConfig = new BotConfig();
            topicConfig.setBot(savedBot);
            topicConfig.setConfigType(ConfigType.TOPIC);
            topicConfig.setConfigValue("technology");
            topicConfig.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            botConfigurationRepository.save(topicConfig);

            BotConfig fetchAmountConfig = new BotConfig();
            fetchAmountConfig.setBot(savedBot);
            fetchAmountConfig.setConfigType(ConfigType.FETCH_AMOUNT);
            fetchAmountConfig.setConfigValue("10");
            fetchAmountConfig.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            botConfigurationRepository.save(fetchAmountConfig);

            BotConfig maxRetriesConfig = new BotConfig();
            maxRetriesConfig.setBot(savedBot);
            maxRetriesConfig.setConfigType(ConfigType.MAX_RETRIES);
            maxRetriesConfig.setConfigValue("3");
            maxRetriesConfig.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            botConfigurationRepository.save(maxRetriesConfig);

            BotConfig scheduleConfig = new BotConfig();
            scheduleConfig.setBot(savedBot);
            scheduleConfig.setConfigType(ConfigType.SCHEDULE_MINUTES);
            scheduleConfig.setConfigValue("30");
            scheduleConfig.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            botConfigurationRepository.save(scheduleConfig);
            
            // Store the bot in userBots map
            userBots.put(chatId, savedBot);
            
            // Clear the user state
            userStates.remove(chatId);
            
            sendMessage(chatId, String.format("✅ Bot '%s' created successfully! Now let's configure it.", botName));
            showConfigMenu(chatId);
            
        } catch (Exception e) {
            log.error("Error creating bot: {}", e.getMessage(), e);
            sendMessage(chatId, "Error creating bot: " + e.getMessage());
            userStates.remove(chatId);
        }
    }

    private void handleConfigMenuCallback(long chatId, String callbackData) {
        try {
            // Get the current bot for this user
            Bot currentBot = userBots.get(chatId);
            if (currentBot == null) {
                sendMessage(chatId, "⚠️ No bot selected. Please create or select a bot first.");
                showMainMenu(chatId);
                return;
            }

            switch (callbackData) {
                case "set_topic":
                    userStates.put(chatId, "waiting_for_topic");
                    sendMessage(chatId, "Please enter the topic for news fetching (e.g., technology, sports, business):");
                    break;
                case "set_fetch_amount":
                    userStates.put(chatId, "waiting_for_fetch_amount");
                    sendMessage(chatId, "Please enter the number of articles to fetch (1-50):");
                    break;
                case "set_schedule":
                    userStates.put(chatId, "waiting_for_schedule");
                    sendMessage(chatId, "Please enter the schedule interval in minutes (e.g., 30 for every 30 minutes):");
                    break;
                case "back_to_main":
                    userStates.remove(chatId);
                    showMainMenu(chatId);
                    break;
                default:
                    sendMessage(chatId, "Invalid option selected.");
                    break;
            }
        } catch (Exception e) {
            log.error("Error handling config menu callback: {}", e.getMessage(), e);
            sendMessage(chatId, "Error processing configuration. Please try again.");
        }
    }

    private void saveBotConfig(long chatId, String configType, String value) {
        try {
            Bot bot = userBots.get(chatId);
            if (bot == null) {
                sendMessage(chatId, "Please create a bot first.");
                userStates.remove(chatId);
                return;
            }

            // Convert string configType to enum
            ConfigType type;
            switch (configType) {
                case "topic":
                    type = ConfigType.TOPIC;
                    break;
                case "fetch_amount":
                    type = ConfigType.FETCH_AMOUNT;
                    break;
                case "schedule_minutes":
                    type = ConfigType.SCHEDULE_MINUTES;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid config type: " + configType);
            }

            // Check if config already exists
            Optional<BotConfig> existingConfig = botConfigurationRepository.findByBotAndConfigType(bot, type);
            BotConfig config;

            if (existingConfig.isPresent()) {
                config = existingConfig.get();
                config.setConfigValue(value);
            } else {
                config = new BotConfig();
                config.setBot(bot);
                config.setConfigType(type);
                config.setConfigValue(value);
                config.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            }

            botConfigurationRepository.save(config);

            // Update scheduler if schedule_minutes was changed
            if (type == ConfigType.SCHEDULE_MINUTES) {
                int interval = Integer.parseInt(value);
                startBotScheduler(bot, interval);
            }

            // Clear user state and show success message
            userStates.remove(chatId);
            sendMessage(chatId, "✅ Configuration saved successfully!");
            showConfigMenu(chatId);

        } catch (Exception e) {
            log.error("Error saving bot configuration: {}", e.getMessage());
            sendMessage(chatId, "Error saving configuration. Please try again.");
            userStates.remove(chatId);
        }
    }
    
    private void stopBotById(long chatId, Long botId) {
        try {
            Bot bot = botService.getBotById(botId);
            if (bot == null) {
                sendMessage(chatId, "Bot not found. It may have been deleted.");
                return;
            }
            
            // Stop the bot's scheduler
            stopBotScheduler(botId);
            
            sendMessage(chatId, String.format("✅ Bot '%s' has been stopped. It will no longer fetch news automatically.", bot.getName()));
            sendWelcomeMessage(chatId);
            
        } catch (Exception e) {
            log.error("Error stopping bot: {}", e.getMessage());
            sendMessage(chatId, "Error stopping bot. Please try again.");
        }
    }
    
    private void deleteBotById(long chatId, Long botId) {
        try {
            Bot bot = botService.getBotById(botId);
            if (bot == null) {
                sendMessage(chatId, "Bot not found. It may have been already deleted.");
                return;
            }
            
            // Stop any scheduled tasks for this bot
            stopBotScheduler(botId);
            
            // Remove from user states and bots if present
            userStates.remove(chatId);
            userBots.remove(chatId);
            
            // Delete the bot from the database
            botService.deleteBot(botId);
            
            sendMessage(chatId, String.format("✅ Bot '%s' has been deleted successfully.", bot.getName()));
            sendWelcomeMessage(chatId);
            
        } catch (Exception e) {
            log.error("Error deleting bot: {}", e.getMessage(), e);
            sendMessage(chatId, "Error deleting bot. Please try again later.");
        }
    }

    private boolean isCommonWord(String word) {
        String[] commonWords = {"the", "and", "for", "with", "that", "this", "have", "from", "are", "were"};
        for (String commonWord : commonWords) {
            if (word.equalsIgnoreCase(commonWord)) {
                return true;
            }
        }
        return false;
    }

    private void showMainMenu(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Create bot button
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton createBotButton = new InlineKeyboardButton();
        createBotButton.setText("Create New Bot");
        createBotButton.setCallbackData("create_bot");
        row1.add(createBotButton);
        keyboard.add(row1);

        // Configure bot button
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton configBotButton = new InlineKeyboardButton();
        configBotButton.setText("Configure Bot");
        configBotButton.setCallbackData("configure_bot");
        row2.add(configBotButton);
        keyboard.add(row2);

        markup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Welcome to Neural News Bot! What would you like to do?");
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error showing main menu: {}", e.getMessage(), e);
        }
    }
}