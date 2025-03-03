package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.Bot;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing bot operations such as creating, updating,
 * retrieving, and deleting bots.
 */
public interface BotService {
    Bot createBot(Bot bot);
    List<Bot> listBots();
    void saveBot(Bot bot);
    Bot getBotByName(String name);
    Bot getBotById(Long botId);
    void deleteBot(Long botId);
    Bot updateBot(Bot bot);
    Optional<Bot> getBot(Long id);
}