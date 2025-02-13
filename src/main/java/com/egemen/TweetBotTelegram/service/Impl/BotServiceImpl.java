package com.egemen.TweetBotTelegram.service.impl;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.repository.BotRepository;
import com.egemen.TweetBotTelegram.service.BotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotServiceImpl implements BotService {
    private final BotRepository botRepository;
    private final Map<String, Boolean> botStates = new ConcurrentHashMap<>();

    @Override
    public Bot createBot(Bot bot) {
        log.info("Creating new bot with name: {}", bot.getName());
        return botRepository.save(bot);
    }

    @Override
    public void startBot(String chatId) {
        log.info("Starting bot for chat ID: {}", chatId);
        botStates.put(chatId, true);
    }

    @Override
    public void stopBot(String chatId) {
        log.info("Stopping bot for chat ID: {}", chatId);
        botStates.put(chatId, false);
    }

    @Override
    public boolean isRunning(String chatId) {
        return botStates.getOrDefault(chatId, false);
    }
}