package com.egemen.TweetBotTelegram.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

@Configuration
public class DotenvConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setProperties(loadDotenvProperties());
        return configurer;
    }

    private static Properties loadDotenvProperties() {
        Properties properties = new Properties();
        Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMissing()
                .systemProperties()
                .load();

        // Load environment variables into properties
        properties.setProperty("MEDIASTACK_API_KEY", dotenv.get("MEDIASTACK_API_KEY"));
        properties.setProperty("GEMINI_API_KEY", dotenv.get("GEMINI_API_KEY"));
        properties.setProperty("PEXELS_API_KEY", dotenv.get("PEXELS_API_KEY"));
        properties.setProperty("INSTAGRAM_ACCESS_TOKEN", dotenv.get("INSTAGRAM_ACCESS_TOKEN"));
        properties.setProperty("INSTAGRAM_USERID", dotenv.get("INSTAGRAM_USERID"));
        properties.setProperty("TELEGRAM_BOT_USERNAME", dotenv.get("TELEGRAM_BOT_USERNAME"));
        properties.setProperty("TELEGRAM_BOT_TOKEN", dotenv.get("TELEGRAM_BOT_TOKEN"));
        properties.setProperty("AWS_ACCESS_KEY", dotenv.get("AWS_ACCESS_KEY"));
        properties.setProperty("AWS_SECRET_KEY", dotenv.get("AWS_SECRET_KEY"));
        properties.setProperty("AWS_S3_BUCKET", dotenv.get("AWS_S3_BUCKET"));
        properties.setProperty("AWS_REGION", dotenv.get("AWS_REGION"));
        properties.setProperty("APP_SCHEDULER_FETCH_NEWS_RATE", dotenv.get("APP_SCHEDULER_FETCH_NEWS_RATE", "300000"));
        properties.setProperty("APP_SCHEDULER_POST_RATE", dotenv.get("APP_SCHEDULER_POST_RATE", "600000"));

        return properties;
    }
}
