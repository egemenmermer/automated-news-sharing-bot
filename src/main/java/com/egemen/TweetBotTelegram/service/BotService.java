package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.dto.NewsArticleDTO;
import java.util.List;

public interface BotService {
    Bot createBot(Bot bot);
    void startBot(String chatId);
    void stopBot(String chatId);
    boolean isRunning(String chatId);
    int fetchLatestNews(String chatId);
    List<NewsArticleDTO> getLatestNews(String chatId, int limit);
    
    // New methods needed by TelegramServiceImpl
    int getPendingPostsCount(String chatId);
    List<InstagramPost> getPendingPosts(String chatId);
    void publishPendingPosts(String chatId);
}
