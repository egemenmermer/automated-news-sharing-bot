package com.egemen.TweetBotTelegram.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class InstagramConfig {

    @Bean
    public String instagramAccessToken() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String token = dotenv.get("INSTAGRAM_ACCESS_TOKEN");
        log.info("Loaded Instagram access token");
        return token;
    }
    
    @Bean
    public String instagramUserId() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String userId = dotenv.get("INSTAGRAM_USERID");
        log.info("Loaded Instagram user ID");
        return userId;
    }
} 