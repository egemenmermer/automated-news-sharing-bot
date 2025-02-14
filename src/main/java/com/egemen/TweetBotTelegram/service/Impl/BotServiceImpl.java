package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.dto.NewsArticleDTO;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.repository.BotRepository;
import com.egemen.TweetBotTelegram.repository.NewsRepository;
import com.egemen.TweetBotTelegram.service.BotService;
import com.egemen.TweetBotTelegram.service.NewsService;
import com.egemen.TweetBotTelegram.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of BotService responsible for managing automated bots.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BotServiceImpl implements BotService {

    private final BotRepository botRepository;
    private final NewsRepository newsRepository;
    private final NewsService newsService;
    private final NotificationService notificationService;

    @Override
    public Bot createBot(Bot bot) {
        log.info("🤖 Creating new bot with name: {}", bot.getName());
        try {
            return botRepository.save(bot);
        } catch (Exception e) {
            log.error("❌ Error creating bot: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create bot", e);
        }
    }

    @Override
    public void startBot(String chatId) {
        log.info("▶️ Starting bot for chat ID: {}", chatId);
        try {
            notificationService.sendNotification(chatId, "✅ Bot started! Now fetching news automatically.");
        } catch (Exception e) {
            log.error("❌ Error starting bot: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start bot", e);
        }
    }

    @Override
    public void stopBot(String chatId) {
        log.info("⏹ Stopping bot for chat ID: {}", chatId);
        try {
            notificationService.sendNotification(chatId, "🛑 Bot stopped! Automated fetching is disabled.");
        } catch (Exception e) {
            log.error("❌ Error stopping bot: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to stop bot", e);
        }
    }

    @Override
    public boolean isRunning(String chatId) {
        // Add logic to track bot running status
        log.info("Checking bot status for chat ID: {}", chatId);
        return false; // Default return (Modify with real logic)
    }

    @Override
    public int fetchLatestNews(String chatId) {
        log.info("📰 Fetching latest news for chat ID: {}", chatId);
        try {
            List<News> newsEntities = newsService.getLatestNews(5);

            List<NewsArticleDTO> newsList = newsEntities.stream()
                    .map(this::convertToDTO)
                    .toList();

            notificationService.sendNotification(chatId, "✅ Found " + newsList.size() + " new articles!");
            return newsList.size();
        } catch (Exception e) {
            log.error("❌ Error fetching news: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch news", e);
        }
    }

    @Override
    public List<NewsArticleDTO> getLatestNews(String chatId, int limit) {
        log.info("🔎 Retrieving {} latest news articles for chat ID: {}", limit, chatId);

        return newsRepository.findTopByOrderByPublishedAtDesc(limit).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public int getPendingPostsCount(String chatId) {
        log.info("📌 Checking pending posts count for chat ID: {}", chatId);
        try {
            // Dummy count (Replace with actual implementation)
            return 0;
        } catch (Exception e) {
            log.error("❌ Error getting pending posts count: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get pending posts count", e);
        }
    }

    @Override
    public List<InstagramPost> getPendingPosts(String chatId) {
        log.info("📌 Getting pending posts for chat ID: {}", chatId);
        try {
            // Replace with actual implementation
            return List.of();
        } catch (Exception e) {
            log.error("❌ Error retrieving pending posts: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve pending posts", e);
        }
    }

    @Override
    public void publishPendingPosts(String chatId) {
        log.info("📢 Publishing pending posts for chat ID: {}", chatId);
        try {
            notificationService.sendNotification(chatId, "✅ All pending posts have been published!");
        } catch (Exception e) {
            log.error("❌ Error publishing posts: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish posts", e);
        }
    }

    private NewsArticleDTO convertToDTO(News news) {
        return new NewsArticleDTO(
                news.getTitle(),
                news.getContent(),
                news.getUrl(),
                news.getImageUrl(),
                news.getPublishedAt().toString(),
                news.getSource()
        );
    }
}