package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.exception.CustomException;
import com.egemen.TweetBotTelegram.repository.BotRepository;
import com.egemen.TweetBotTelegram.service.BotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of BotService responsible for managing automated bots.
 */
@Slf4j
@Service
public class BotServiceImpl implements BotService {

    private final BotRepository botRepository;

    public BotServiceImpl(BotRepository botRepository) {
        this.botRepository = botRepository;
    }

    @Override
    public Bot createBot(Bot bot) {
        if(bot.getName() == null) {
            throw new CustomException(new Exception("Bot Name is required"), HttpStatus.BAD_REQUEST);
        }
        try {
            return botRepository.save(bot);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(e, HttpStatus.BAD_REQUEST);
        } catch(Exception e) {
            throw new CustomException(new Exception("Error creating bot: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<Bot> listBots() {
        List<Bot> bots = botRepository.findAll();
        if(bots.isEmpty()) {
            throw new CustomException(new Exception("No Bots found"), HttpStatus.NOT_FOUND);
        }
        return bots;
    }

    @Override
    public void saveBot(Bot bot) {
        try {
            botRepository.save(bot);
        } catch (Exception e) {
            throw new CustomException(e, HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public Bot getBotByName(String name) {
        return botRepository.findByName(name)
                .orElseThrow(() -> new CustomException(new Exception("Bot not found"), HttpStatus.NOT_FOUND));
    }

    @Override
    public Bot getBotById(Long botId) {
        return botRepository.findById(botId)
                .orElseThrow(() -> new CustomException(new Exception("Bot not found"), HttpStatus.NOT_FOUND));
    }

    @Override
    public void deleteBot(Long botId) {
        try {
            Bot bot = getBotById(botId);
            botRepository.delete(bot);
        } catch (Exception e) {
            throw new CustomException(new Exception("Error deleting bot: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}