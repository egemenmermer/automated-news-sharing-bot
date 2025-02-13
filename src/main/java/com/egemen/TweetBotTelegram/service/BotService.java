package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.Bot;

public interface BotService {
    /**
     * Create a new bot
     */
    Bot createBot(Bot bot);

    /**
     * Start a bot with the given chat ID
     */
    void startBot(String chatId);

    /**
     * Stop a bot with the given chat ID
     */
    void stopBot(String chatId);

    /**
     * Check if a bot is running for a specific chat ID
     */
    boolean isRunning(String chatId);
}
