package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.Bot;
import com.egemen.TweetBotTelegram.entity.InstagramPost;
import com.egemen.TweetBotTelegram.dto.NewsArticleDTO;

import java.util.List;

/**
 * Service for managing bot operations such as starting, stopping,
 * fetching news, and handling pending posts.
 */
public interface BotService {

    /**
     * Create a new bot.
     *
     * @param bot The bot entity to create.
     * @return Created bot entity.
     */
    Bot createBot(Bot bot);

    /**
     * Start the bot for the given chat ID.
     *
     * @param chatId The chat ID where the bot will operate.
     */
    void startBot(String chatId);

    /**
     * Stop the bot for the given chat ID.
     *
     * @param chatId The chat ID to stop the bot.
     */
    void stopBot(String chatId);

    /**
     * Check if the bot is currently running for the given chat ID.
     *
     * @param chatId The chat ID to check.
     * @return true if the bot is running, false otherwise.
     */
    boolean isRunning(String chatId);

    /**
     * Fetch the latest news for a specific chat.
     *
     * @param chatId The chat ID requesting news.
     * @return The number of new articles fetched.
     */
    int fetchLatestNews(String chatId);

    /**
     * Retrieve the latest news articles.
     *
     * @param chatId The chat ID requesting news.
     * @param limit The number of latest articles to fetch.
     * @return List of latest news articles.
     */
    List<NewsArticleDTO> getLatestNews(String chatId, int limit);

    /**
     * Get the count of pending posts for the given chat ID.
     *
     * @param chatId The chat ID to check for pending posts.
     * @return Number of pending posts.
     */
    int getPendingPostsCount(String chatId);

    /**
     * Retrieve the list of pending Instagram posts.
     *
     * @param chatId The chat ID requesting pending posts.
     * @return List of pending Instagram posts.
     */
    List<InstagramPost> getPendingPosts(String chatId);

    /**
     * Publish all pending Instagram posts.
     *
     * @param chatId The chat ID requesting publication.
     */
    void publishPendingPosts(String chatId);
}