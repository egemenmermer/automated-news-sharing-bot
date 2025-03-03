package com.egemen.TweetBotTelegram.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class DotenvConfig {

    @Bean
    public Dotenv dotenv(ConfigurableEnvironment environment) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        
        Map<String, Object> envMap = new HashMap<>();
        
        // Add database configuration
        addProperty(envMap, dotenv, "DB_URL", "spring.datasource.url");
        addProperty(envMap, dotenv, "DB_USERNAME", "spring.datasource.username");
        addProperty(envMap, dotenv, "DB_PASSWORD", "spring.datasource.password");
        
        // Add AWS S3 configuration
        addProperty(envMap, dotenv, "AWS_ACCESS_KEY", "aws.s3.accessKey");
        addProperty(envMap, dotenv, "AWS_SECRET_KEY", "aws.s3.secretKey");
        addProperty(envMap, dotenv, "AWS_S3_BUCKET", "aws.s3.bucketName");
        addProperty(envMap, dotenv, "AWS_REGION", "aws.s3.region");
        
        // Add Instagram configuration
        addProperty(envMap, dotenv, "INSTAGRAM_USERID", "instagram.userId");
        addProperty(envMap, dotenv, "INSTAGRAM_ACCESS_TOKEN", "instagram.accessToken");
        
        // Add other properties as needed
        addProperty(envMap, dotenv, "PEXELS_API_KEY", "pexels.api.key");
        addProperty(envMap, dotenv, "MEDIASTACK_API_KEY", "mediastack.api.key");
        addProperty(envMap, dotenv, "GEMINI_API_KEY", "gemini.api.key");
        addProperty(envMap, dotenv, "TELEGRAM_BOT_USERNAME", "telegram.bot.username");
        addProperty(envMap, dotenv, "TELEGRAM_BOT_TOKEN", "telegram.bot.token");
        
        // Add the property source to the environment
        environment.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", envMap));
        
        log.info("Loaded environment variables from .env file");
        return dotenv;
    }
    
    private void addProperty(Map<String, Object> envMap, Dotenv dotenv, String envKey) {
        addProperty(envMap, dotenv, envKey, envKey);
    }
    
    private void addProperty(Map<String, Object> envMap, Dotenv dotenv, String envKey, String propertyKey) {
        String value = dotenv.get(envKey);
        if (value != null) {
            envMap.put(propertyKey, value);
        }
    }
}
