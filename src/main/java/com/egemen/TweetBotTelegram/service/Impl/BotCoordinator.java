package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.service.Impl.TelegramServiceImpl;
import com.egemen.TweetBotTelegram.service.NewsService;
import com.egemen.TweetBotTelegram.service.GeminiService;
import com.egemen.TweetBotTelegram.service.InstagramService;
import com.egemen.TweetBotTelegram.service.Impl.ImageGenerationServiceImpl;
import com.egemen.TweetBotTelegram.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Slf4j
@Service
public class BotCoordinator {
    private final NewsService newsService;
    private final GeminiService geminiService;
    private final InstagramService instagramService;
    private final TelegramServiceImpl telegramService;
    private final ImageGenerationServiceImpl imageGenerationService;
    private final S3Service s3Service;
    
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private static final long HOUR_IN_MILLISECONDS = 3600000; // 1 hour

    @Autowired
    public BotCoordinator(NewsService newsService, GeminiService geminiService, InstagramService instagramService, TelegramServiceImpl telegramService, ImageGenerationServiceImpl imageGenerationService, S3Service s3Service) {
        this.newsService = newsService;
        this.geminiService = geminiService;
        this.instagramService = instagramService;
        this.telegramService = telegramService;
        this.imageGenerationService = imageGenerationService;
        this.s3Service = s3Service;
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
            if (chatId != null) telegramService.sendMessage(chatId, "🔄 Fetching latest news...");
            List<News> newsList = newsService.fetchNews();

            if (newsList.isEmpty()) {
                if (chatId != null) telegramService.sendMessage(chatId, "No new articles found.");
                return;
            }

            for (News news : newsList) {
                try {
                    String summary = geminiService.summarizeNews(news.getTitle(), news.getContent());
                    String caption = geminiService.generateCaption(news.getTitle(), summary);
                    
                    // Generate image using HuggingFace
                    String imagePath = imageGenerationService.generateNewsImage(news.getTitle(), summary);
                    
                    if (imagePath != null) {
                        // Upload to S3
                        try (InputStream imageStream = new FileInputStream(new File(imagePath))) {
                            String s3Url = s3Service.uploadImage(imageStream, 
                                "news-" + news.getId() + ".png");
                            
                            // Create Instagram post
                            instagramService.createPost(news.getTitle(), caption, s3Url);
                            
                            // Send update to Telegram
                            if (chatId != null) {
                                telegramService.sendNewsUpdate(chatId, news.getTitle(), summary, s3Url);
                            }
                            
                            // Update news
                            news.setProcessed(true);
                            news.setImageUrl(s3Url);
                            newsService.updateNews(news);
                        }
                    }
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