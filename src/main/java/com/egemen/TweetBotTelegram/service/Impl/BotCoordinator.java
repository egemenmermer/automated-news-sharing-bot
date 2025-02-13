package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.service.Impl.TelegramServiceImpl;
import com.egemen.TweetBotTelegram.service.NewsService;
import com.egemen.TweetBotTelegram.service.GeminiService;
import com.egemen.TweetBotTelegram.service.InstagramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class BotCoordinator {
    private final NewsService newsService;
    private final GeminiService geminiService;
    private final InstagramService instagramService;
    private final TelegramServiceImpl telegramService;
    
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private static final long HOUR_IN_MILLISECONDS = 3600000; // 1 hour

    @Autowired
    public BotCoordinator(NewsService newsService, GeminiService geminiService, InstagramService instagramService, TelegramServiceImpl telegramService) {
        this.newsService = newsService;
        this.geminiService = geminiService;
        this.instagramService = instagramService;
        this.telegramService = telegramService;
    }

    public void startBot(String chatId) {
        if (isRunning.compareAndSet(false, true)) {
            telegramService.sendMessage(chatId, "🟢 Bot started! Will fetch and post news hourly.");
            processAndPostNews(chatId);
        } else {
            telegramService.sendMessage(chatId, "Bot is already running!");
        }
    }

    public void stopBot(String chatId) {
        if (isRunning.compareAndSet(true, false)) {
            telegramService.sendMessage(chatId, "🔴 Bot stopped!");
        } else {
            telegramService.sendMessage(chatId, "Bot is already stopped!");
        }
    }

    @Scheduled(fixedRate = HOUR_IN_MILLISECONDS)
    public void scheduledNewsProcessing() {
        if (isRunning.get()) {
            processAndPostNews(null);
        }
    }

    private void processAndPostNews(String chatId) {
        try {
            // 1. Fetch news
            if (chatId != null) telegramService.sendMessage(chatId, "🔄 Fetching latest news...");
            List<News> newsList = newsService.fetchNews();

            if (newsList.isEmpty()) {
                if (chatId != null) telegramService.sendMessage(chatId, "No new articles found.");
                return;
            }

            // 2. Process each news item
            for (News news : newsList) {
                try {
                    // Generate content
                    String summary = geminiService.summarizeNews(news.getTitle(), news.getContent());
                    String caption = geminiService.generateCaption(news.getTitle(), summary);
                    String imagePrompt = geminiService.generateImagePrompt(news.getTitle(), summary);

                    // Create and publish Instagram post
                    instagramService.createPost(news.getTitle(), caption, news.getImageUrl() != null ? news.getImageUrl() : imagePrompt);

                    // Send update to Telegram if chatId is provided
                    if (chatId != null) {
                        telegramService.sendNewsUpdate(chatId, news.getTitle(), summary, news.getImageUrl());
                    }

                    // Mark news as processed
                    news.setProcessed(true);
                    newsService.updateNews(news);

                } catch (Exception e) {
                    log.error("Error processing news item: {}", news.getTitle(), e);
                    if (chatId != null) {
                        telegramService.sendError(chatId, "Error processing news: " + news.getTitle());
                    }
                }
            }

            if (chatId != null) {
                telegramService.sendMessage(chatId, String.format("✅ Processed %d news articles", newsList.size()));
            }

        } catch (Exception e) {
            log.error("Error in news processing cycle", e);
            if (chatId != null) {
                telegramService.sendError(chatId, "Error in news processing: " + e.getMessage());
            }
        }
    }

    public boolean isRunning() {
        return isRunning.get();
    }
} 