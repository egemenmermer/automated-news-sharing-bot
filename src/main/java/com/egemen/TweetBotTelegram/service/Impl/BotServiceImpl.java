package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.exception.CustomException;
import com.egemen.TweetBotTelegram.repository.*;
import com.egemen.TweetBotTelegram.service.BotService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of BotService responsible for managing automated bots.
 */
@Slf4j
@Service
public class BotServiceImpl implements BotService {

    @PersistenceContext
    private EntityManager entityManager;

    private final BotRepository botRepository;
    private final FetchLogsRepository fetchLogsRepository;
    private final BotConfigRepository botConfigRepository;
    private final NewsRepository newsRepository;
    private final SummarizedNewsRepository summarizedNewsRepository;
    private final PostLogsRepository postLogsRepository;
    private final BotLogsRepository botLogsRepository;

    @Autowired
    public BotServiceImpl(BotRepository botRepository,
                         FetchLogsRepository fetchLogsRepository,
                         BotConfigRepository botConfigRepository,
                         NewsRepository newsRepository,
                         SummarizedNewsRepository summarizedNewsRepository,
                         PostLogsRepository postLogsRepository,
                         BotLogsRepository botLogsRepository) {
        this.botRepository = botRepository;
        this.fetchLogsRepository = fetchLogsRepository;
        this.botConfigRepository = botConfigRepository;
        this.newsRepository = newsRepository;
        this.summarizedNewsRepository = summarizedNewsRepository;
        this.postLogsRepository = postLogsRepository;
        this.botLogsRepository = botLogsRepository;
        log.info("BotServiceImpl initialized");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Bot createBot(Bot bot) {
        log.info("Creating bot: {}", bot.getName());
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Bot updateBot(Bot bot) {
        log.info("Updating bot: {}", bot.getName());
        return botRepository.save(bot);
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Optional<Bot> getBot(Long id) {
        log.info("Getting bot with ID: {}", id);
        return botRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Bot> listBots() {
        log.info("Listing all bots");
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
    @Transactional
    public void deleteBot(Long botId) {
        try {
            Bot bot = getBotById(botId);
            // Log the deletion attempt
            log.info("Deleting bot: {}", bot.getName());
            
            // Delete the bot
            botRepository.deleteById(botId);
            log.info("Bot deleted successfully: {}", botId);
        } catch (Exception e) {
            log.error("Error deleting bot with ID {}: {}", botId, e.getMessage());
            throw new RuntimeException("Failed to delete bot: " + e.getMessage(), e);
        }
    }
}