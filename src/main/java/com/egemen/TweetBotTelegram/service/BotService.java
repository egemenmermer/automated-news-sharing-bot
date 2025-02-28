package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.Bot;

import java.util.List;

/**
 * Service for managing bot operations such as starting, stopping,
 * fetching news, and handling pending posts.
 */
public interface BotService {
    Bot createBot(Bot bot);
    List<Bot> listBots(Long userId);
    void saveBot(Bot bot);
    Bot getBotByUserId(Long userId);
    Bot getBotById(Long botId);
}