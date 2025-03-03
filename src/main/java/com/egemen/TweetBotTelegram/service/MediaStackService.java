package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.News;
import java.util.List;

public interface MediaStackService {
    
    /**
     * Fetches news from the MediaStack API
     * 
     * @param categories Categories to fetch news for
     * @param limit Maximum number of news items to fetch
     * @return List of News objects
     */
    List<News> fetchNews(String categories, int limit);
    
    /**
     * Fetches news from the MediaStack API with default parameters
     * 
     * @return List of News objects
     */
    List<News> fetchNews();
} 