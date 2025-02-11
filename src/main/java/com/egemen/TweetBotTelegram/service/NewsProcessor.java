package com.egemen.TweetBotTelegram.service;

public interface NewsProcessor {
    /**
     * Process latest news articles
     * Fetches news, generates summaries, finds images, creates posts
     */
    void processNews();

    /**
     * Publish pending posts to Instagram
     */
    void publishPendingPosts();
} 