package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.News;
import java.util.List;

/**
 * Service for managing news operations such as fetching, updating, processing,
 * and deleting news articles.
 */
public interface NewsService {

    /**
     * Fetch news from an external API and save it to the database.
     *
     * @return List of newly fetched news articles.
     */
    List<News> fetchNews();

    /**
     * Update an existing news article.
     *
     * @param news The news entity to update.
     */
    void updateNews(News news);

    /**
     * Retrieve the latest news articles with a specified limit.
     *
     * @param limit The number of latest articles to fetch.
     * @return List of latest news articles.
     */
    List<News> getLatestNews(int limit);

    /**
     * Retrieve all unprocessed news articles.
     *
     * @return List of unprocessed news articles.
     */
    List<News> getUnprocessedNews();

    /**
     * Retrieve a specific news article by its ID.
     *
     * @param id The ID of the news article.
     * @return The news article entity.
     */
    News getNews(Long id);

    /**
     * Delete a news article by its ID.
     *
     * @param id The ID of the news article to delete.
     */
    void deleteNews(Long id);

    /**
     * Save a new or updated news article.
     *
     * @param news The news entity to save.
     * @return The saved news entity.
     */
    News save(News news);

    /**
     * Process the image for a given news article.
     *
     * @param news The news entity whose image needs processing.
     * @return The updated news entity.
     */
    News processNewsImage(News news);

    /**
     * Mark a news article as posted.
     *
     * @param news The news entity to update.
     */
    void markAsPosted(News news);

    /**
     * Process a news article by summarizing its content.
     *
     * @param news The news entity to process.
     */
    void processNews(News news);
}