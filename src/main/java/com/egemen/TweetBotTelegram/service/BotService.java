package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.Bot;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing bot operations such as creating, updating,
 * retrieving, and deleting bots.
 */
public interface BotService {
    Bot saveBot(Bot bot);
    Bot getBotById(Long id);
    Bot getBotByName(String name);
    List<Bot> listBots();
    void deleteBot(Long id);
    Optional<Bot> getBot(Long id);
    Bot createBot(Bot bot);
    Bot updateBot(Bot bot);
}