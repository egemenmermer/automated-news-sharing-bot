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
@Transactional
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
        return botRepository.findAll();
    }

    @Override
    public Bot saveBot(Bot bot) {
        return botRepository.save(bot);
    }

    @Override
    public Bot getBotByName(String name) {
        return botRepository.findByName(name)
            .orElse(null);
    }

    @Override
    public Bot getBotById(Long id) {
        return botRepository.findById(id)
            .orElse(null);
    }

    @Override
    @Transactional
    public void deleteBot(Long id) {
        try {
            log.info("Deleting bot with ID: {}", id);
            
            // First delete all related configurations
            botConfigRepository.deleteByBotId(id);
            
            // Then delete all related news
            newsRepository.deleteByBotId(id);
            
            // Delete all related fetch logs
            fetchLogsRepository.deleteByBotId(id);
            
            // Delete all related post logs
            postLogsRepository.deleteByBotId(id);
            
            // Delete all related bot logs
            botLogsRepository.deleteByBotId(id);
            
            // Finally delete the bot
            botRepository.deleteById(id);
            
            log.info("Successfully deleted bot and all related entities with ID: {}", id);
        } catch (Exception e) {
            log.error("Error deleting bot with ID {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to delete bot: " + e.getMessage(), e);
        }
    }
}