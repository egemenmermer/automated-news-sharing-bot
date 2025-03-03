package com.egemen.TweetBotTelegram.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MediaStackConfig {

    @Bean
    public String mediaStackApiKey() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String apiKey = dotenv.get("MEDIASTACK_API_KEY");
        log.info("Loaded MediaStack API key");
        return apiKey;
    }
} 