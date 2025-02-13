package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.News;
import java.util.List;

public interface NewsService {
    /**
     * Fetch news from external API
     */
    List<News> fetchNews();

    /**
     * Update existing news
     */
    void updateNews(News news);

    /**
     * Get latest news with limit
     */
    List<News> getLatestNews(int limit);

    /**
     * Get unprocessed news
     */
    List<News> getUnprocessedNews();

    /**
     * Get news by ID
     */
    News getNews(Long id);

    /**
     * Delete news by ID
     */
    void deleteNews(Long id);

    /**
     * Save news
     */
    News save(News news);

    /**
     * Process news image
     */
    News processNewsImage(News news);

    /**
     * Mark news as posted
     */
    void markAsPosted(News news);

    /**
     * Process news content
     */
    void processNews(News news);
}
