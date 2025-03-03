package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.enums.ConfigType;

import java.util.List;

/**
 * Service for managing news operations such as fetching, updating, processing,
 * and deleting news articles.
 */
public interface NewsService {

    List<News> fetchAndSaveNews(Long botId, boolean isTR);
    String getBotConfig(Bot bot, ConfigType configType);
    List<News> getAllNews();
    List<News> getPendingNews(Long botId);
}