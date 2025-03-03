package com.egemen.TweetBotTelegram.scheduler;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.repository.BotRepository;
import com.egemen.TweetBotTelegram.service.NewsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduler responsible for fetching news articles from MediaStack API
 * at regular intervals for all active bots.
 */
@Slf4j
@Component
public class NewsFetchScheduler {

    @Value("${APP_SCHEDULER_FETCH_NEWS_RATE:300000}")
    private long fetchNewsRate;

    private final NewsService newsService;
    private final BotRepository botRepository;

    public NewsFetchScheduler(NewsService newsService, BotRepository botRepository) {
        this.newsService = newsService;
        this.botRepository = botRepository;
    }

    /**
     * Scheduled task that runs at a fixed rate to fetch news for all active bots.
     * The rate is configurable via the APP_SCHEDULER_FETCH_NEWS_RATE environment variable.
     */
    // Disabled automatic scheduling to only allow manual fetching through Telegram
    public void fetchNewsForAllBots() {
        log.info("Starting manual news fetch for all bots");
        List<Bot> activeBots = botRepository.findAllActiveBots();
        
        if (activeBots.isEmpty()) {
            log.warn("No active bots found for news fetching");
            return;
        }
        
        log.info("Found {} active bots for news fetching", activeBots.size());
        
        for (Bot bot : activeBots) {
            try {
                log.info("Fetching news for bot: {} ({})", bot.getId(), bot.getName());
                List<News> fetchedNews = newsService.fetchAndSaveNews(bot.getId(), false);
                log.info("Successfully fetched {} news articles for bot: {}", fetchedNews.size(), bot.getId());
                
                // Turkish news fetching temporarily disabled due to database schema updates
                // if (Boolean.TRUE.equals(bot.getIsFetchTurkish())) {
                //     List<News> fetchedTrNews = newsService.fetchAndSaveNews(bot.getId(), true);
                //     log.info("Successfully fetched {} Turkish news articles for bot: {}", fetchedTrNews.size(), bot.getId());
                // }
            } catch (Exception e) {
                log.error("Error fetching news for bot {}: {}", bot.getId(), e.getMessage(), e);
            }
        }
        
        log.info("Completed scheduled news fetch for all bots");
    }
}