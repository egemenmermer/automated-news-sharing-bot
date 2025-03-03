package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.enums.ConfigType;
import com.egemen.TweetBotTelegram.enums.NewsStatus;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing news operations such as fetching, updating, processing,
 * and deleting news articles.
 */
public interface NewsService {

    List<News> fetchAndSaveNews(Long botId, boolean isTR);
    String getBotConfig(Bot bot, ConfigType configType);
    List<News> getAllNews();
    List<News> getPendingNews(Long botId);
    News saveNews(News news);
    Optional<News> getNewsById(Long id);
    void deleteNews(Long id);
    List<News> getNewsByStatus(NewsStatus status);
    List<News> getNewsByBot(Bot bot);
    List<News> getNewsByBotAndStatus(Bot bot, NewsStatus status);
    void updateNewsStatus(Long id, NewsStatus status);
    void markNewsAsPosted(Long id);
    void markNewsAsFailed(Long id);
    List<News> getPendingNews(Bot bot, int limit);
}