package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.exception.CustomException;
import com.egemen.TweetBotTelegram.repository.*;
import com.egemen.TweetBotTelegram.service.BotService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    @Transactional
    public void deleteBot(Long botId) {
        try {
            Bot bot = getBotById(botId);
            // Log the deletion attempt
            log.info("Attempting to delete bot: {} (ID: {})", bot.getName(), bot.getId());
            
            try {
                // Delete all related records in the correct order to avoid foreign key constraint violations
                log.info("Deleting related records for bot: {} (ID: {})", bot.getName(), bot.getId());
                
                // Use JPQL to delete related records by bot
                // This approach is more efficient than fetching and deleting individual records
                
                // Delete instagram posts first to avoid foreign key constraints
                log.info("Deleting instagram posts for bot ID: {}", bot.getId());
                entityManager.createQuery("DELETE FROM InstagramPost ip WHERE ip.bot.id = :botId")
                    .setParameter("botId", bot.getId())
                    .executeUpdate();
                
                // Also delete any Instagram posts that reference news from this bot
                log.info("Deleting instagram posts referencing news from bot ID: {}", bot.getId());
                entityManager.createQuery("DELETE FROM InstagramPost ip WHERE ip.news IN (SELECT n FROM News n WHERE n.bot.id = :botId)")
                    .setParameter("botId", bot.getId())
                    .executeUpdate();

                // Delete fetch logs
                log.info("Deleting fetch logs for bot ID: {}", bot.getId());
                entityManager.createQuery("DELETE FROM FetchLogs f WHERE f.bot.id = :botId")
                    .setParameter("botId", bot.getId())
                    .executeUpdate();
                
                // Delete post logs
                log.info("Deleting post logs for bot ID: {}", bot.getId());
                entityManager.createQuery("DELETE FROM PostLogs p WHERE p.bot.id = :botId")
                    .setParameter("botId", bot.getId())
                    .executeUpdate();
                
                // Delete bot logs
                log.info("Deleting bot logs for bot ID: {}", bot.getId());
                entityManager.createQuery("DELETE FROM BotLogs b WHERE b.bot.id = :botId")
                    .setParameter("botId", bot.getId())
                    .executeUpdate();
                
                // Delete summarized news
                log.info("Deleting summarized news for bot ID: {}", bot.getId());
                entityManager.createQuery("DELETE FROM SummarizedNews s WHERE s.bot.id = :botId")
                    .setParameter("botId", bot.getId())
                    .executeUpdate();
                
                // Delete news
                log.info("Deleting news for bot ID: {}", bot.getId());
                entityManager.createQuery("DELETE FROM News n WHERE n.bot.id = :botId")
                    .setParameter("botId", bot.getId())
                    .executeUpdate();
                
                // Delete bot configurations
                log.info("Deleting bot configurations for bot ID: {}", bot.getId());
                entityManager.createQuery("DELETE FROM BotConfig bc WHERE bc.bot.id = :botId")
                    .setParameter("botId", bot.getId())
                    .executeUpdate();
                
                // Finally delete the bot
                botRepository.delete(bot);
                log.info("Successfully deleted bot: {} (ID: {})", bot.getName(), bot.getId());
            } catch (DataIntegrityViolationException e) {
                log.error("Failed to delete bot due to data integrity violation: {}", e.getMessage());
                throw new CustomException(new Exception("Cannot delete bot due to existing dependencies"), HttpStatus.CONFLICT);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while deleting bot: {}", e.getMessage(), e);
            throw new CustomException(new Exception("Error deleting bot: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}