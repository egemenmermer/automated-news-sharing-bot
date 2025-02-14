package com.egemen.TweetBotTelegram.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@PropertySource(value = "file:.env", ignoreResourceNotFound = true)
public class EnvConfig {
    private static final Logger log = LoggerFactory.getLogger(EnvConfig.class);
    
    private final Environment env;

    public EnvConfig(Environment env) {
        this.env = env;
    }

    @PostConstruct
    public void logEnvVariables() {
        log.info("✅ Environment Variables Loaded:");
        
        // Database
        logVariable("DB_URL");
        logVariable("DB_USERNAME");
        logVariable("DB_PASSWORD");
        
        // AWS
        logVariable("AWS_ACCESS_KEY");
        logVariable("AWS_SECRET_KEY");
        logVariable("AWS_REGION");
        logVariable("AWS_S3_BUCKET");
        
        // APIs
        logVariable("PEXELS_API_KEY");
        logVariable("INSTAGRAM_ACCESS_TOKEN");
        logVariable("MEDIASTACK_API_KEY");
        
        // Scheduler
        logVariable("APP_SCHEDULER_MAX_RETRIES");
        logVariable("APP_SCHEDULER_FETCH_DELAY");
        logVariable("APP_SCHEDULER_POST_DELAY");
        logVariable("APP_SCHEDULER_RETRY_DELAY");
    }

    private void logVariable(String name) {
        String value = env.getProperty(name);
        log.info("{}: {}", name, value != null ? "✓" : "✗");
    }
}