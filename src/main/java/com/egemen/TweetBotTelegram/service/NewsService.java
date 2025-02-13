package com.egemen.TweetBotTelegram.service;

import com.egemen.TweetBotTelegram.entity.News;
import java.util.List;

public interface NewsService {
    List<News> getAllNews();
    List<News> getUnprocessedNews();
    News getNews(Long id);
    void fetchLatestNews();
    void deleteNews(Long id);
    News save(News news);
    News processNewsImage(News news);
    void markAsPosted(News news);
    void processNews(News news);
}
