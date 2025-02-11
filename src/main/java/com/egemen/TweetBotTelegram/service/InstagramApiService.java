package com.egemen.TweetBotTelegram.service;

public interface InstagramApiService {
    /**
     * Upload image to Instagram
     * @param imageUrl URL of the processed image
     * @return Container ID from Instagram
     */
    String uploadMedia(String imageUrl);

    /**
     * Publish the uploaded media
     * @param containerId Container ID from uploadMedia
     * @param caption Post caption
     * @return Instagram post ID
     */
    String publishMedia(String containerId, String caption);

    /**
     * Check media status
     * @param containerId Container ID from uploadMedia
     * @return Status of the media (FINISHED, IN_PROGRESS, ERROR)
     */
    String getMediaStatus(String containerId);
}
