package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.BotConfig;
import com.egemen.TweetBotTelegram.entity.User;
import com.egemen.TweetBotTelegram.enums.ConfigType;
import com.egemen.TweetBotTelegram.repository.BotConfigRepository;
import com.egemen.TweetBotTelegram.repository.PostLogsRepository;
import com.egemen.TweetBotTelegram.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Service;
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
    private final UserService userService;
    private final PostLogsRepository postLogsRepository;
    private TaskScheduler taskScheduler;
    private ScheduledFuture<?> scheduledTask;

    @Value("${telegram.bot.username}")
    private String botUsername;
    @Value("${telegram.bot.token}")
    private String botToken;
    private boolean isBotRunning = false;
    private Long activeChatId = null;
    private String country;

    private Map<Long, String> userStates = new ConcurrentHashMap<>();
    private Map<Long, String> countryMap = new HashMap<>();
    private Map<Long, User> userMap = new HashMap<>();
    private Map<Long, Long> userBotIdMap = new HashMap<>();
    private Map<Long, String> userPostTimes = new HashMap<>();
    private Map<Long, BotConfig> botConfigurationMap = new HashMap<>();
    private Map<Long, Boolean> isworking = new HashMap<>();
    private Map<Long, String> nextWorkTime = new HashMap<>();
    private Map<Long, String> cronbot = new HashMap<>();
    private boolean isTr;
    private String statuste = "Bot başlatıldı.";
    private int haberCekmeSayisi = 0;

    @Autowired
    public TelegramBotService(@Value("${telegram.bot.token}") String botToken, NewsService newsService, InstagramApiService instagramApiService, GeminiService geminiService, BotService botService, BotConfigRepository botConfigurationRepository, UserService userService, PostLogsRepository postLogsRepository) {
        super(botToken);
        this.newsService = newsService;
        this.instagramApiService = instagramApiService;
        this.geminiService = geminiService;
        this.botService = botService;
        this.botConfigurationRepository = botConfigurationRepository;
        this.userService = userService;
        this.postLogsRepository = postLogsRepository;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
            log.info("Telegram bot başarıyla başlatıldı!");
        } catch (TelegramApiException e) {
            log.error("Telegram bot başlatma hatası:", e);
        }
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
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update);
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        log.info("handleCallbackQuery çağrıldı"); // Log eklendi
        String callbackData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        activeChatId = chatId;

        // CallbackData değerini kullanıcının durumu olarak ayarla
        userStates.put(chatId, callbackData);
        log.info("Kullanıcı durumu: {}", userStates.get(chatId));

        // Duruma göre işlem yap
        handleState(chatId, callbackData, null);
    }

    public void notifySuccess(String message) {
        if (activeChatId != null) {
            sendMessage(activeChatId, "✅ " + message);
        }
    }

    public void notifyError(String message) {
        if (activeChatId != null) {
            sendMessage(activeChatId, "❌ " + message);
        }
    }

    private void handleStatusCommand(Long chatId) {
        Long botId = userBotIdMap.get(chatId);
        Bot bot = botService.getBotById(botId);
        long totalPosted = postLogsRepository.countByBot(bot);

        List<BotConfig> configurations = botConfigurationRepository.findBotConfigurationsByBot(bot);

        String status = isBotRunning ? "Çalışıyor ✅" : "Durdu ⛔";

        // Bot bilgilerini oluştur
        StringBuilder botInfo = new StringBuilder();
        botInfo.append("📌 Bot Bilgileri:\n");
        botInfo.append("🔹 Bot Numarası: " + bot.getId() + "\n");
        botInfo.append("🔹 Bot Adı: " + bot.getName() + "\n");
        botInfo.append("🔹 Son Çalıştırma Zamanı: " + bot.getLastRun() + "\n");
        botInfo.append("🔹 Bot Durumu: " + status + "\n");

        // Konfigürasyon bilgilerini ekle
        String postTime = getConfigValue(configurations, ConfigType.POST_TIME) == null
                ? "Saat Başı"
                : convertCronToReadable(getConfigValue(configurations, ConfigType.POST_TIME));

        String fetchTime = getConfigValue(configurations, ConfigType.FETCH_TIME) == null
                ? "Saat Başı"
                : convertCronToReadable(getConfigValue(configurations, ConfigType.FETCH_TIME));

        String maxRetries = getConfigValue(configurations, ConfigType.MAX_RETRIES) == null
                ? "3"
                : getConfigValue(configurations, ConfigType.MAX_RETRIES);

        String topic = getConfigValue(configurations, ConfigType.TOPIC) == null
                ? "Belirtilmemiş"
                : getConfigValue(configurations, ConfigType.TOPIC);

        String fetchAmount = getConfigValue(configurations, ConfigType.FETCH_AMOUNT) == null
                ? "10"
                : getConfigValue(configurations, ConfigType.FETCH_AMOUNT);

        botInfo.append("🔹 Konu: " + topic + "\n");
        botInfo.append("🔹 Max Yeniden Deneme Sayısı: " + maxRetries + "\n");
        botInfo.append("🔹 Haber Çekme Sıklığı: " + fetchTime + "\n");
        botInfo.append("🔹 Postlama Sıklığı: " + postTime + "\n");
        botInfo.append("🔹 Çekilecek Haber Sayısı: " + fetchAmount + "\n");
        botInfo.append("🔹 Toplam Post Edilen Haber Sayısı: " + totalPosted);
        botInfo.append("🔹 Sonraki Çalışma Zamanı: " + nextWorkTime.get(chatId));


        sendMessage(chatId, botInfo.toString());
    }

    private void handleStopCommand(long chatId) {
        log.info("Stop işlemi çağrıldı");
        if (isBotRunning) {
            isBotRunning = false;
            isworking.put(chatId, false);
            activeChatId = null;

            if (scheduledTask != null) {
                scheduledTask.cancel(false);
                log.info("Bot durduruldu.");
            }
            sendMessage(chatId, "Bot durduruldu!");
        } else {
            sendMessage(chatId, "Bot zaten durdurulmuş durumda!");
        }
    }

    private void handleBotOperations() {
        log.info("Bot işlemleri çağrıldı");
        Bot bot1 = botService.getBotById(userBotIdMap.get(activeChatId));
        List<BotConfig> configurations = botConfigurationRepository.findBotConfigurationsByBot(bot1);
        String fetchamount = getConfigValue(configurations, ConfigType.FETCH_AMOUNT) == null ? "10" : getConfigValue(configurations, ConfigType.FETCH_AMOUNT);
        bot1.setLastRun(Timestamp.valueOf(LocalDateTime.now()));

        try {
            log.info("Haberler alınıyor");
            newsService.fetchAndSaveNews(userBotIdMap.get(activeChatId), isTr);
            haberCekmeSayisi++;
            log.info("Haberler işleniyor");
            geminiService.start();
            log.info("Instagram işlemleri çalışıyor");
            instagramApiService.processAndPostToInstagram(userBotIdMap.get(activeChatId).intValue());
            log.info("Bot işlemleri başarıyla çalıştı");
            if (haberCekmeSayisi >= Integer.parseInt(fetchamount)) {
                log.info("Maksimum haber çekme sayısına ulaşıldı. Bir sonraki planlanmış zamana kadar bekleniyor.");
                statuste = "Maksimum haber çekme sayısına ulaşıldı. Bir sonraki çalışma zamanı: " + getNextExecutionTime();
                haberCekmeSayisi = 0; // Sayaç sıfırlanır
            } else {
                statuste = "Bot çalışıyor. Sonraki çalışma zamanı: " + getNextExecutionTime();
            }
        } catch (InterruptedException e) {
            log.error("Bot işlemleri sırasında hata oluştu:", e.getMessage());
            Thread.currentThread().interrupt(); // Thread'in interrupt durumunu sıfırla
            handleStopCommand(activeChatId);
        }
    }

    private void handleStartBotCommand(long chatId) {
        log.info("Bot başlatılıyor");
        isBotRunning = true;
        activeChatId = chatId;
        isworking.put(chatId, true);
        Bot bot = botService.getBotById(userBotIdMap.get(chatId));
        List<BotConfig> configurations = botConfigurationRepository.findBotConfigurationsByBot(bot);
        String postTimeforbot = getConfigValue(configurations, ConfigType.POST_TIME);
        userPostTimes.put(chatId, postTimeforbot);
        String cronExpression = postTimeforbot != null ? postTimeforbot : "0 0 * * * *";
        cronbot.put(chatId, cronExpression);
        haberCekmeSayisi = 0;
        while (isBotRunning) {
            handleBotOperations();

            // Bir sonraki planlanmış zamana kadar bekle
            scheduledTask = taskScheduler.schedule(this::handleBotOperations, new CronTrigger(cronExpression));
            updateNextExecutionTime(cronExpression);

            // Status bilgisini güncelle
            statuste = "Bot standby modunda. Sonraki çalışma zamanı: " + getNextExecutionTime();
            log.info(statuste);

            // Bir sonraki çalışma zamanına kadar bekle
            try {
                Thread.sleep(getMillisUntilNextExecution(cronExpression)); // Bir sonraki çalışma zamanına kadar bekle
            } catch (InterruptedException e) {
                log.error("Bekleme sırasında hata oluştu:", e);
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void handleMessage(Update update) {
        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        // Kullanıcının mevcut durumunu al
        String currentState = userStates.getOrDefault(chatId, "WAITING_AUTHENTICATION");

        // Duruma göre işlem yap
        handleState(chatId, currentState, messageText);
    }


    private void handleState(Long chatId, String state, String messageText) {
        log.info("handleState çağrıldı, Durum: {}, Mesaj: {}", state, messageText);

        if (state.startsWith("/selectBot_")) { // Bu satır eklendi
            Long botId = Long.parseLong(state.split("_")[1]); // Bot ID'yi al
            userBotIdMap.put(chatId, botId);
            sendMessage(chatId, "Bot ID " + botId + " seçildi.");
            userStates.put(chatId, "READY");
            handleReadyCommand(chatId);
            return; // Metottan çık (önemli)
        }
        switch (state) {
            case "/login":
                userStates.put(chatId, "WAITING_USERNAME");
                break;
            case "/logout":
                userStates.remove(chatId);
                userBotIdMap.remove(chatId);
                sendMessage(chatId, "Çıkış yapıldı!");
                userStates.put(chatId, "WAITING_AUTHENTICATION");
                break;
            case "/createUser":
                userStates.put(chatId, "WAITING_CREATE_USER");
                break;
            case "/status":
                handleStatusCommand(chatId);
                break;
            case "/startbot":
                userStates.put(chatId, "/country");
                break;
            case "/country":
                if (messageText == null) {
                    sendMessage(chatId, "Lütfen haber alınacak ülke seçiniz,(Dünya veya Türkiye)");
                    return;
                }
                if (Stream.of("Türkiye", "World", "Dunya", "Dünya").noneMatch(country -> country.equalsIgnoreCase(messageText))) {
                    sendMessage(chatId, "Geçersiz bir ülke bilgisi girdiniz. Lütfen tekrar deneyin.");
                    userStates.put(chatId, "/country");
                    return;
                }
                country = (messageText.equalsIgnoreCase("türkiye") || messageText.equalsIgnoreCase("turkiye"))
                        ? "Türkiye"
                        : "Dünya";

                isTr = country.equals("Türkiye") ? true : false;
                countryMap.put(chatId, country);
                sendMessage(chatId, "Ülke: " + messageText + " seçildi.");
                userStates.put(chatId, "/bot");
                log.info("New state set to: {}", userStates.get(chatId));
                handleState(chatId, "/bot", messageText);
                break;
            case "/bot":
                log.info("Bot case reached, starting bot command");
                handleStartBotCommand(chatId);
                break;
            case "/stop":
                handleStopCommand(chatId);
                break;
            case "WAITING_AUTHENTICATION":
                if (messageText != null && messageText.equals("/start")) {
                    // Butonları oluştur
                    InlineKeyboardButton loginButton = new InlineKeyboardButton();
                    loginButton.setText("Giriş Yap");
                    loginButton.setCallbackData("WAITING_USERNAME");

                    InlineKeyboardButton createUserButton = new InlineKeyboardButton();
                    createUserButton.setText("Kayıt Ol");
                    createUserButton.setCallbackData("WAITING_CREATE_USER");

                    InlineKeyboardButton UsersButton = new InlineKeyboardButton();
                    UsersButton.setText("Kullanıcılar");
                    UsersButton.setCallbackData("LIST_USERS");

                    // Butonları bir liste içine ekle
                    List<InlineKeyboardButton> row = new ArrayList<>();
                    row.add(loginButton);
                    row.add(createUserButton);
                    row.add(UsersButton);

                    // Buton listesini bir üst listeye ekle
                    List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
                    buttons.add(row);

                    // Butonlu mesajı gönder
                    sendInlineKeyboard(chatId, buttons, "Hesabınız var mı kontrol edin? Giriş yapın veya kayıt olun.");
                }
                break;

            case "WAITING_USERNAME":
                if (messageText != null) {
                    List<User> users = userService.getAllUsers();
                    Optional<User> userForTelegram = users.stream()
                            .filter(user -> user.getUsername().equalsIgnoreCase(messageText)) // Büyük/küçük harf duyarsız karşılaştır
                            .findFirst();

                    if (userForTelegram.isPresent()) {
                        userMap.put(chatId, userForTelegram.get());
                        List<Bot> userBots = botService.listBots(userForTelegram.get().getId());

                        if (userBots.isEmpty()) {
                            sendMessage(chatId, "Hiç botunuz yok. Lütfen yeni bir bot oluşturun.");
                            userStates.put(chatId, "WAITING_BOT_CREATE");
                        } else {
                            listUserBots(chatId, userForTelegram.get());
                        }
                    } else {
                        sendMessage(chatId, "Kullanıcı adı bulunamadı, tekrar deneyin:");
                        userStates.put(chatId, "WAITING_USERNAME");
                    }
                } else {
                    sendMessage(chatId, "Kullanıcı adınızı girin:");
                }
                break;

            case "WAITING_CREATE_USER":
                sendMessage(chatId, "Kayıt için kullanıcı adınızı girin:");
                if (messageText != null) {
                    User user = new User();
                    user.setUsername(messageText);
                    userService.createUser(user);
                    userStates.put(chatId, "WAITING_AUTHENTICATION");
                    sendMessage(chatId, "Kullanıcı başarıyla oluşturuldu. Giriş yapmak için '/login' komutunu kullanın.");
                }
                break;
            case "LIST_USERS":
                List<User> users = userService.getAllUsers();
                StringBuilder usersList = new StringBuilder();
                usersList.append("📌 Kullanıcı Listesi:\n");
                for (User user : users) {
                    usersList.append("🔹 Kullanıcı Adı: " + user.getUsername() + "\n");
                }
                sendMessage(chatId, usersList.toString());
                break;

            case "WAITING_BOT_CREATE":
                if (messageText != null) {
                    sendMessage(chatId, "Bot ismini girin:");
                    userStates.put(chatId, "WAITING_BOT_NAME");
                }
                break;

            case "WAITING_BOT_NAME":
                if (messageText != null) {
                    User userForBot = userMap.get(chatId);
                    Bot botCreated = new Bot();
                    botCreated.setName(messageText);
                    botCreated.setUser(userForBot);
                    botService.createBot(botCreated);
                    sendMessage(chatId, "Bot adı: " + messageText + "\n Şimdi lütfen bot post ve fetch zamanını girin (30 dakikada 1, Saat Başı, 2,3,4,5,6,7,8 saatte 1)");
                    userStates.put(chatId, "WAITING_POST_TIME");
                }
                break;

            case "WAITING_POST_TIME":
                if (messageText != null) {
                    if (Stream.of("30 dakikada 1", "Saat Başı", "2 saatte 1", "3 saatte 1", "4 saatte 1", "5 saatte 1", "6 saatte 1", "7 saatte 1", "8 saatte 1").noneMatch(postTime -> postTime.equalsIgnoreCase(messageText))) {
                        sendMessage(chatId, "Geçersiz bir post ve fetch zamanı seçtiniz. Lütfen tekrar deneyin." + "\n Şimdi lütfen bot post zamanını girin (30 dakikada 1, Saat Başı, 2,3,4,5,6,7,8 saatte 1)");
                        userStates.put(chatId, "WAITING_POST_TIME");
                        break;
                    }
                    saveBotConfiguration(chatId, ConfigType.POST_TIME, messageText);
                    saveBotConfiguration(chatId, ConfigType.FETCH_TIME, messageText);
                    userPostTimes.put(chatId, messageText);
                    sendMessage(chatId, "Bot Post ve Fetch Zamanı: " + messageText + "\nŞimdi lütfen Topic bilgisi girin" + "\n(General,Sports,Politics,Health,Science,Business,Technology");
                    userStates.put(chatId, "WAITING_TOPIC");
                }
                break;

            case "WAITING_TOPIC":
                if (messageText != null) {
                    if (Stream.of("General", "Sports", "Politics", "Health", "Science", "Business", "Technology").noneMatch(topic -> topic.equalsIgnoreCase(messageText))) {
                        sendMessage(chatId, "Geçersiz bir konu seçtiniz. Lütfen tekrar deneyin." + "\n(General,Sports,Politics,Health,Science,Business,Technology");
                        userStates.put(chatId, "WAITING_TOPIC");
                        break;
                    }
                    saveBotConfiguration(chatId, ConfigType.TOPIC, messageText);
                    sendMessage(chatId, "Topic: " + messageText + "\nŞimdi lütfen Max Tekrar Deneme sayısını girin. (1-10)");
                    userStates.put(chatId, "WAITING_MAX_RETRIES");
                }
                break;

            case "WAITING_MAX_RETRIES":
                if (messageText != null) {
                    sendMessage(chatId, "Max Tekrar Deneme Sayısı: " + messageText + "\nŞimdi ülke bilgisini girin(World veya Türkiye.");
                    saveBotConfiguration(chatId, ConfigType.MAX_RETRIES, messageText);
                    userStates.put(chatId, "WAITING_COUNTRY");
                }
                break;

            case "WAITING_COUNTRY":
                if (messageText != null) {
                    if (Stream.of("Türkiye", "World", "Turkiye", "Dunya", "Dünya", "Turkey").noneMatch(country -> country.equalsIgnoreCase(messageText))) {
                        sendMessage(chatId, "Geçersiz bir ülke bilgisi girdiniz. Lütfen tekrar deneyin.");
                        userStates.put(chatId, "WAITING_COUNTRY");
                        break;
                    }
                    String country = messageText.toUpperCase();
                    sendMessage(chatId, "Bot adı: " + messageText + ", Ülke: " + country + " seçildi.");
                    sendMessage(chatId, "Konfigürasyon tamamlandı!");
                    listUserBots(chatId, userMap.get(chatId));
                }
                break;

            case "READY":
                handleReadyCommand(chatId);
                break;

            default:
                sendMessage(chatId, "Bilinmeyen işlem.");
        }
    }

    private void sendInlineKeyboard(Long chatId, List<List<InlineKeyboardButton>> buttons, String messageText) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(buttons);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageText);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Butonlu mesaj gönderme hatası:", e);
        }
    }

    private void listUserBots(Long chatId, User user) {
        List<Bot> userBots = botService.listBots(user.getId());

        if (userBots.isEmpty()) {
            sendMessage(chatId, "Hiç botunuz yok. Lütfen yeni bir bot oluşturun.");
            userStates.put(chatId, "WAITING_BOT_CREATE");
        } else {
            List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
            for (Bot bot : userBots) {
                List<BotConfig> botConfigurations = botConfigurationRepository.findBotConfigurationsByBot(bot);
                InlineKeyboardButton botButton = new InlineKeyboardButton();
                botButton.setText("Bot ID: " + bot.getId() + " - " + bot.getName());
                botButton.setCallbackData("/selectBot_" + bot.getId());

                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(botButton);
                buttons.add(row);
            }
            sendInlineKeyboard(chatId, buttons, "Botlarınız:");
        }
    }

    private void saveBotConfiguration(Long chatId, ConfigType configType, String configValue) {
        Bot bot = botService.getBotByUserId(userMap.get(chatId).getId());
        BotConfig botConfig = new BotConfig();
        botConfig.setBot(bot);
        botConfig.setConfigType(configType);
        botConfig.setConfigValue(configValue);
        botConfigurationRepository.save(botConfig);
    }

    public String getConfigValue(List<BotConfig> configurations, ConfigType configType) {
        return configurations.stream()
                .filter(config -> config.getConfigType() == configType)
                .map(BotConfig::getConfigValue)
                .findFirst()
                .orElse("Bilgi Yok");  // Eğer değer yoksa "Bilgi Yok" döner
    }

    private String convertCronToReadable(String cronExpression) {
        if (cronExpression == null || cronExpression.isEmpty()) {
            return "Bilinmiyor";
        }

        String[] parts = cronExpression.split(" ");
        if (parts.length < 5) {
            return "Geçersiz cron formatı";
        }

        String minute = parts[0];
        String hour = parts[1];
        String day = parts[2];
        String month = parts[3];
        String weekday = parts[4];

        if ("*".equals(minute) && "*".equals(hour)) {
            return "Her dakika";
        } else if ("0".equals(minute) && "*".equals(hour)) {
            return "Saat başı";
        } else if ("0".equals(minute) && hour.matches("\\d+")) {
            return hour + ":00'da";
        } else if (minute.matches("\\d+") && hour.matches("\\d+")) {
            return hour + ":" + minute + "'de";
        } else if (hour.contains(",")) {
            return "Saat " + hour.replace(",", ", ") + " arasında";
        } else if ("*".equals(hour) && minute.matches("\\d+")) {
            return "Her saat " + minute + ". dakikada";
        } else if (hour.equals("*") && minute.equals("0")) {
            return "Her saat başında";
        } else if (minute.equals("*") && hour.matches("\\d+")) {
            return hour + " ile her dakika";
        }

        return "Cron ifadesi: " + cronExpression;
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Mesaj gönderme hatası:", e);
        }
    }

    private void handleReadyCommand(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Lütfen bir işlem seçin:");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Butonlar
        InlineKeyboardButton startBotButton = new InlineKeyboardButton("StartBot");
        startBotButton.setCallbackData("/country");

        InlineKeyboardButton stopButton = new InlineKeyboardButton("Stop");
        stopButton.setCallbackData("/stop");

        InlineKeyboardButton statusButton = new InlineKeyboardButton("Status");
        statusButton.setCallbackData("/status");

        InlineKeyboardButton logoutButton = new InlineKeyboardButton("Logout");
        logoutButton.setCallbackData("/logout");

        InlineKeyboardButton createUserButton = new InlineKeyboardButton("Create User");
        createUserButton.setCallbackData("/createUser");

        InlineKeyboardButton loginButton = new InlineKeyboardButton("Login");
        loginButton.setCallbackData("/login");


        // Butonları satırlara ekle
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(startBotButton);
        row1.add(stopButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(statusButton);
        row2.add(logoutButton);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createUserButton);
        row3.add(loginButton);

        rowsInline.add(row1);
        rowsInline.add(row2);
        rowsInline.add(row3);

        keyboardMarkup.setKeyboard(rowsInline);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Butonlu mesaj gönderme hatası:", e);
        }
    }

    void updateNextExecutionTime(String cronExpression) {
        // CronTrigger kullanarak bir sonraki çalışma zamanını hesapla
        CronTrigger trigger = new CronTrigger(cronExpression);
        Instant nextExecutionInstant = trigger.nextExecution(new SimpleTriggerContext());

        // Instant'ı Date'e çevir (isteğe bağlı)
        Date nextExecutionTime = Date.from(nextExecutionInstant);

        // Tarihi formatla
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.systemDefault());
        String formattedTime = formatter.format(nextExecutionInstant);

        // Sonraki çalışma zamanını kaydet
        nextWorkTime.put(activeChatId, formattedTime);
    }

    private long getMillisUntilNextExecution(String cronExpression) {
        CronTrigger trigger = new CronTrigger(cronExpression);
        Instant nextExecutionInstant = trigger.nextExecution(new SimpleTriggerContext());
        return nextExecutionInstant.toEpochMilli() - System.currentTimeMillis();
    }

    private String getNextExecutionTime() {
        CronTrigger trigger = new CronTrigger(cronbot.get(activeChatId));
        Instant nextExecutionInstant = trigger.nextExecution(new SimpleTriggerContext());

        // Tarihi formatla
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.systemDefault());
        return formatter.format(nextExecutionInstant);
    }

}