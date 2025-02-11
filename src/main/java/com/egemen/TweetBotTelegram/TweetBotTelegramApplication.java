package com.egemen.TweetBotTelegram;

import com.egemen.TweetBotTelegram.config.EnvConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableScheduling
@EnableRetry
public class TweetBotTelegramApplication {

	public static void main(String[] args) {
		SpringApplication.run(TweetBotTelegramApplication.class, args);
	}

	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(25);
		executor.setThreadNamePrefix("SocialMedia-");
		executor.initialize();
		return executor;
	}

	@Bean("mainRestTemplate")
	@Primary
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
