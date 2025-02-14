package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.dto.NewsArticleDTO;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.repository.BotRepository;
import com.egemen.TweetBotTelegram.repository.NewsRepository;
import com.egemen.TweetBotTelegram.repository.InstagramPostRepository;
import com.egemen.TweetBotTelegram.service.BotService;
import com.egemen.TweetBotTelegram.service.InstagramService;
import com.egemen.TweetBotTelegram.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotServiceImpl implements BotService {
    private final BotRepository botRepository;
    private final NewsRepository newsRepository;
    private final InstagramPostRepository instagramPostRepository;
    private final InstagramService instagramService;
    private final NewsService newsService;
    private final Map<String, Boolean> botStates = new ConcurrentHashMap<>();
    private final Map<String, ScheduledExecutorService> schedulers = new ConcurrentHashMap<>();

    @Override
    public Bot createBot(Bot bot) {
        log.info("Creating new bot with name: {}", bot.getName());
        try {
            return botRepository.save(bot);
        } catch (Exception e) {
            log.error("Error creating bot: {}", e.getMessage());
            throw new RuntimeException("Failed to create bot", e);
        }
    }

    @Override
    public void startBot(String chatId) {
        log.info("Starting bot for chat ID: {}", chatId);
        try {
            botStates.put(chatId, true);
            
            // Create a new scheduler for this chat
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            schedulers.put(chatId, scheduler);
            
            // Schedule news fetching and posting every hour
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    if (isRunning(chatId)) {
                        fetchAndProcessNews(chatId);
                        publishPendingPosts(chatId);
                    }
                } catch (Exception e) {
                    log.error("Error in scheduled task for chat {}: {}", chatId, e.getMessage());
                }
            }, 0, 1, TimeUnit.HOURS);
            
        } catch (Exception e) {
            log.error("Error starting bot for chat {}: {}", chatId, e.getMessage());
            throw new RuntimeException("Failed to start bot", e);
        }
    }

    @Override
    public void stopBot(String chatId) {
        log.info("Stopping bot for chat ID: {}", chatId);
        try {
            botStates.put(chatId, false);
            
            // Shutdown the scheduler if it exists
            ScheduledExecutorService scheduler = schedulers.get(chatId);
            if (scheduler != null) {
                scheduler.shutdown();
                schedulers.remove(chatId);
            }
        } catch (Exception e) {
            log.error("Error stopping bot for chat {}: {}", chatId, e.getMessage());
            throw new RuntimeException("Failed to stop bot", e);
        }
    }

    @Override
    public boolean isRunning(String chatId) {
        return botStates.getOrDefault(chatId, false);
    }

    @Override
    public int fetchLatestNews(String chatId) {
        log.info("Fetching latest news for chat ID: {}", chatId);
        try {
            return fetchAndProcessNews(chatId);
        } catch (Exception e) {
            log.error("Error fetching news: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch news", e);
        }
    }

    private int fetchAndProcessNews(String chatId) {
        List<News> newArticles = newsRepository.fetchLatestNews();
        int processedCount = 0;
        
        for (News news : newArticles) {
            try {
                newsService.processNews(news);
                processedCount++;
            } catch (Exception e) {
                log.error("Error processing news article: {}", e.getMessage());
            }
        }
        
        return processedCount;
    }

    @Override
    public List<NewsArticleDTO> getLatestNews(String chatId, int limit) {
        log.info("Getting {} latest news articles for chat ID: {}", limit, chatId);
        try {
            return newsRepository.findLatestNewsByLimit(limit)
                    .stream()
                    .map(this::convertToDTO)
                    .toList();
        } catch (Exception e) {
            log.error("Error retrieving latest news: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve latest news", e);
        }
    }

    @Override
    public int getPendingPostsCount(String chatId) {
        log.info("Getting pending posts count for chat ID: {}", chatId);
        try {
            return instagramPostRepository.countPendingPosts();
        } catch (Exception e) {
            log.error("Error getting pending posts count: {}", e.getMessage());
            throw new RuntimeException("Failed to get pending posts count", e);
        }
    }

    @Override
    public List<InstagramPost> getPendingPosts(String chatId) {
        log.info("Getting pending posts for chat ID: {}", chatId);
        try {
            return instagramPostRepository.findPendingPosts();
        } catch (Exception e) {
            log.error("Error getting pending posts: {}", e.getMessage());
            throw new RuntimeException("Failed to get pending posts", e);
        }
    }

    @Override
    public void publishPendingPosts(String chatId) {
        log.info("Publishing pending posts for chat ID: {}", chatId);
        try {
            instagramService.publishPendingPosts();
        } catch (Exception e) {
            log.error("Error publishing pending posts: {}", e.getMessage());
            throw new RuntimeException("Failed to publish pending posts", e);
        }
    }

    private NewsArticleDTO convertToDTO(News news) {
        NewsArticleDTO dto = new NewsArticleDTO();
        dto.setTitle(news.getTitle());
        dto.setDescription(news.getContent());
        dto.setUrl(news.getUrl());
        dto.setImage(news.getImageUrl());
        dto.setPublishedAt(news.getPublishedAt().toString());
        dto.setSource(news.getSource());
        return dto;
    }
}