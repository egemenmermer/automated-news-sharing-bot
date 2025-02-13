package com.egemen.TweetBotTelegram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TweetBotTelegramApplication {

	public static void main(String[] args) {
		SpringApplication.run(TweetBotTelegramApplication.class, args);
	}

}
