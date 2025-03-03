package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.News;

public interface NewsImageService {
    /**
     * Generates an image for a news article
     * @param news The news article
     * @return The URL of the generated image
     */
    String generateImageForNews(News news);
    
    /**
     * Generates an image based on a prompt and title
     * @param imagePrompt The prompt for image generation
     * @param title The title of the news article
     * @return The URL of the generated image
     */
    String generateNewsImage(String imagePrompt, String title);
    
    /**
     * Downloads an image from a URL and saves it locally
     * @param imageUrl The URL of the image to download
     * @return The local path to the downloaded image
     */
    String downloadImage(String imageUrl);
    
    String generateAndUploadImage(News news);
} 