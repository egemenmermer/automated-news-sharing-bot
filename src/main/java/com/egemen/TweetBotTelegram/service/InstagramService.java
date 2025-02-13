package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.InstagramPost;
import java.util.List;

public interface InstagramService {
    /**
     * Get all Instagram posts
     */
    List<InstagramPost> getAllPosts();

    /**
     * Get posts pending publication
     */
    List<InstagramPost> getPendingPosts();

    /**
     * Get a specific post by ID
     */
    InstagramPost getPost(Long id);

    /**
     * Create a new Instagram post from news
     */
    InstagramPost createPost(Long newsId, String imageUrl);

    /**
     * Create a new Instagram post directly
     */
    void createPost(String title, String caption, String imageUrl);

    /**
     * Publish a pending post to Instagram
     */
    InstagramPost publishPost(Long id);

    /**
     * Delete a post
     */
    void deletePost(Long id);

    /**
     * Publish all pending posts
     */
    void publishPendingPosts();
}