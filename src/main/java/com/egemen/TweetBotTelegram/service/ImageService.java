package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.News;

public interface ImageService {
    /**
     * Find or fetch an appropriate image for the news
     */
    String findImageForNews(News news);

    /**
     * Create a search query for image APIs
     */
    String createSearchQuery(String title, String content);

    /**
     * Search for an image using Pexels API
     */
    String searchPexelsImage(String query);

    /**
     * Validate if an image meets Instagram requirements
     */
    boolean isValidForInstagram(String imageUrl);

    /**
     * Process and prepare an image for Instagram posting
     */
    String processImageForInstagram(String imageUrl);

    /**
     * Download and validate an image from URL
     */
    boolean validateImage(String imageUrl, int minWidth, int minHeight);
}