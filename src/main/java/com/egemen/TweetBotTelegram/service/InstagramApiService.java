package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.exception.InstagramApiException;

public interface InstagramApiService {
    /**
     * Uploads an image to Instagram and returns a media container ID.
     * @param imageUrl The URL of the processed image.
     * @param caption The caption for the post.
     * @return The media container ID from Instagram.
     * @throws InstagramApiException If the upload fails.
     */
    String uploadImageToInstagram(String imageUrl, String caption) throws InstagramApiException;

    /**
     * Publishes the uploaded media on Instagram.
     * @param mediaId The media container ID obtained from uploadImageToInstagram.
     * @return True if the post was successfully published, false otherwise.
     * @throws InstagramApiException If publishing fails.
     */
    boolean publishPost(String mediaId) throws InstagramApiException;

    /**
     * Refreshes the Instagram access token.
     */
    void refreshAccessToken();
}