package com.egemen.TweetBotTelegram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;

@SpringBootApplication
@EnableScheduling
public class TweetBotTelegramApplication {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().load();
        for (DotenvEntry entry : dotenv.entries()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }
        SpringApplication.run(TweetBotTelegramApplication.class, args);
    }
}
