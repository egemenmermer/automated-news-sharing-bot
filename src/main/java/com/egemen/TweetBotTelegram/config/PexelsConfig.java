package com.egemen.TweetBotTelegram.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class PexelsConfig {

    @Bean
    public String pexelsApiKey() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String apiKey = dotenv.get("PEXELS_API_KEY");
        log.info("Loaded Pexels API key");
        return apiKey;
    }
} 