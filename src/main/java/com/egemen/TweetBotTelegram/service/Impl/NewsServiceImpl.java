package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.dto.NewsArticleDTO;
import com.egemen.TweetBotTelegram.dto.NewsResponseDTO;
import com.egemen.TweetBotTelegram.entity.News;
import com.egemen.TweetBotTelegram.repository.NewsRepository;
import com.egemen.TweetBotTelegram.service.GeminiService;
import com.egemen.TweetBotTelegram.service.NewsService;
import com.egemen.TweetBotTelegram.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of NewsService responsible for fetching and processing news.
 */
@Slf4j
@Service
public class NewsServiceImpl implements NewsService {

    private final RestTemplate restTemplate;
    private final String mediaStackApiKey;
    private final NewsRepository newsRepository;
    private final GeminiService geminiService;
    private final NotificationService notificationService;

    @Autowired
    public NewsServiceImpl(
            RestTemplate restTemplate,
            @Value("${mediastack.api.key}") String mediaStackApiKey,
            NewsRepository newsRepository,
            GeminiService geminiService,
            NotificationService notificationService) {
        this.restTemplate = restTemplate;
        this.mediaStackApiKey = mediaStackApiKey;
        this.newsRepository = newsRepository;
        this.geminiService = geminiService;
        this.notificationService = notificationService;
    }

    @Override
    public List<News> fetchNews() {
        try {
            String url = buildMediaStackUrl();
            NewsResponseDTO response = restTemplate.getForObject(url, NewsResponseDTO.class);

            if (response != null && response.getData() != null) {
                List<News> newsList = new ArrayList<>();
                for (NewsArticleDTO article : response.getData()) {
                    News news = convertToNews(article);
                    if (!newsExists(news)) {
                        newsList.add(newsRepository.save(news));
                    }
                }
                log.info("📢 Fetched {} new articles", newsList.size());
                return newsList;
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("❌ Error fetching news: {}", e.getMessage());
            notificationService.sendError("YOUR_CHAT_ID", "Failed to fetch news.");
            throw new RuntimeException("Failed to fetch news", e);
        }
    }

    @Override
    public void updateNews(News news) {
        newsRepository.save(news);
    }

    @Override
    public List<News> getLatestNews(int limit) {
        return newsRepository.findTopByOrderByPublishedAtDesc(limit);
    }

    @Override
    public List<News> getUnprocessedNews() {
        return newsRepository.findByProcessedFalse();
    }

    @Override
    public News getNews(Long id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found with id: " + id));
    }

    @Override
    public void deleteNews(Long id) {
        newsRepository.deleteById(id);
    }

    @Override
    public News save(News news) {
        return newsRepository.save(news);
    }

    @Override
    public News processNewsImage(News news) {
        if (news.getImageUrl() != null && !news.isProcessed()) {
            try {
                news.setProcessed(true);
                return newsRepository.save(news);
            } catch (Exception e) {
                log.error("Error processing image for news {}: {}", news.getId(), e.getMessage());
            }
        }
        return news;
    }

    @Override
    public void markAsPosted(News news) {
        news.setPosted(true);
        newsRepository.save(news);
    }

    @Override
    public void processNews(News news) {
        try {
            String summary = geminiService.summarizeNews(news.getTitle(), news.getContent());
            news.setSummary(summary);
            news.setProcessed(true);
            newsRepository.save(news);

            notificationService.sendNotification("YOUR_CHAT_ID", "📰 " + news.getTitle() + "\n" + summary);
        } catch (Exception e) {
            log.error("❌ Error processing news: {}", e.getMessage(), e);
            notificationService.sendError("YOUR_CHAT_ID", "Error processing news.");
        }
    }

    private boolean newsExists(News news) {
        return newsRepository.existsByTitleAndContent(news.getTitle(), news.getContent());
    }

    private String buildMediaStackUrl() {
        return String.format("http://api.mediastack.com/v1/news?access_key=%s&languages=en&limit=10", mediaStackApiKey);
    }

    private News convertToNews(NewsArticleDTO article) {
        News news = new News();
        news.setTitle(article.getTitle());
        news.setContent(article.getDescription());
        news.setUrl(article.getUrl());
        news.setImageUrl(article.getImage());
        news.setSource(article.getSource());
        news.setPublishedAt(LocalDateTime.now());
        news.setProcessed(false);
        news.setPosted(false);
        return news;
    }
}