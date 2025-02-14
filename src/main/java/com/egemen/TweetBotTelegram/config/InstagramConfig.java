package com.egemen.TweetBotTelegram.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@PropertySource(value = ".env", ignoreResourceNotFound = true)
public class InstagramConfig {

    @Value("${INSTAGRAM_USERNAME}")
    private String username;

    @Value("${INSTAGRAM_PASSWORD}")
    private String password;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}