package com.egemen.TweetBotTelegram.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private SocialMediaConfig socialMedia;
    private SecurityConfig security;
    private SchedulerConfig scheduler;
    private InstagramConfig instagram;

    @Data
    public static class SocialMediaConfig {
        private PlatformConfig twitter;
        private PlatformConfig telegram;
    }

    @Data
    public static class PlatformConfig {
        private int maxRetries;
        private int postDelay;
        private int batchSize;
    }

    @Data
    public static class SecurityConfig {
        private String apiKey;
        private RateLimit rateLimit;
    }

    @Data
    public static class RateLimit {
        private int requests;
        private int perMinutes;
    }

    @Data
    public static class SchedulerConfig {
        private int fetchNewsRate;
        private int postRate;
    }

    @Data
    public static class InstagramConfig {
        private int maxRetries = 3;
        private int postDelay = 60;
        private String captionTemplate = "📰 {title}\n\n{description}\n\n#news #update";
    }
}