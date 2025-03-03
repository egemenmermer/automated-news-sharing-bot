package com.egemen.TweetBotTelegram.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TelegramConfig {

    @Bean
    public String telegramBotUsername() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String username = dotenv.get("TELEGRAM_BOT_USERNAME");
        log.info("Loaded Telegram bot username");
        return username;
    }
    
    @Bean
    public String telegramBotToken() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String token = dotenv.get("TELEGRAM_BOT_TOKEN");
        log.info("Loaded Telegram bot token");
        return token;
    }
} 