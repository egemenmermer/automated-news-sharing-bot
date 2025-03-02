package com.egemen.TweetBotTelegram.service;

public interface PexelsService {
    /**
     * Search and fetch an image from Pexels API based on the given query
     * @param query The search query for finding relevant images
     * @return byte array containing the image data
     * @throws Exception if there's an error fetching or processing the image
     */
    byte[] searchAndFetchImage(String query) throws Exception;
}