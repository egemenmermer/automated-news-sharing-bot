package com.egemen.TweetBotTelegram.config;

import com.egemen.TweetBotTelegram.service.Impl.TelegramServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramServiceImpl telegramService) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(telegramService);
        return api;
    }
} 